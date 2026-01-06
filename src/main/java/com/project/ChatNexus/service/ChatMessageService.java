package com.project.ChatNexus.service;

import com.project.ChatNexus.dto.response.ChatContactResponse;
import com.project.ChatNexus.model.ChatMessage;
import com.project.ChatNexus.model.MessageStatus;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service handling chat message operations including saving,
 * retrieving, and managing message status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    /**
     * Save a chat message.
     *
     * @param chatMessage the message to save
     * @return the saved message
     */
    public ChatMessage save(ChatMessage chatMessage) {
        log.debug("Saving message from {} to {}", chatMessage.getSenderId(), chatMessage.getRecipientId());

        var chatId = chatRoomService.getChatRoomId(
                chatMessage.getSenderId(),
                chatMessage.getRecipientId(),
                true).orElseThrow(() -> {
                    log.error("Failed to get/create chat room for {} and {}",
                            chatMessage.getSenderId(), chatMessage.getRecipientId());
                    return new RuntimeException("Failed to create chat room");
                });

        chatMessage.setChatId(chatId);
        chatMessage.setTimeStamp(new Date());

        if (userService.isUserOnline(chatMessage.getRecipientId())) {
            chatMessage.setStatus(MessageStatus.DELIVERED);
            log.debug("Recipient {} is online, setting status to DELIVERED", chatMessage.getRecipientId());
        } else {
            chatMessage.setStatus(MessageStatus.SENT);
            log.debug("Recipient {} is offline, setting status to SENT", chatMessage.getRecipientId());
        }

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("Message saved with ID: {}", savedMessage.getId());
        return savedMessage;
    }

    /**
     * Find all messages between two users.
     *
     * @param senderId    the sender's ID
     * @param recipientId the recipient's ID
     * @return list of messages
     */
    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        log.debug("Finding messages between {} and {}", senderId, recipientId);
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        List<ChatMessage> messages = chatId.map(chatMessageRepository::findByChatId).orElse(new ArrayList<>());
        log.debug("Found {} messages", messages.size());
        return messages;
    }

    /**
     * Find undelivered messages for a user.
     *
     * @param recipientId the recipient's ID
     * @return list of undelivered messages
     */
    public List<ChatMessage> findUndeliveredMessages(String recipientId) {
        log.debug("Finding undelivered messages for {}", recipientId);
        List<ChatMessage> messages = chatMessageRepository.findByRecipientIdAndStatus(recipientId, MessageStatus.SENT);
        log.debug("Found {} undelivered messages", messages.size());
        return messages;
    }

    /**
     * Mark multiple messages as delivered.
     *
     * @param messages list of messages to mark
     */
    public void markMessagesAsDelivered(List<ChatMessage> messages) {
        log.debug("Marking {} messages as delivered", messages.size());
        messages.forEach(msg -> {
            msg.setStatus(MessageStatus.DELIVERED);
            chatMessageRepository.save(msg);
        });
        log.info("Marked {} messages as DELIVERED", messages.size());
    }

    /**
     * Mark a single message as delivered.
     *
     * @param messageId the message ID
     */
    public void markMessageAsDelivered(String messageId) {
        log.debug("Marking message {} as delivered", messageId);
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus(MessageStatus.DELIVERED);
            chatMessageRepository.save(msg);
            log.debug("Message {} marked as DELIVERED", messageId);
        });
    }

    /**
     * Mark all messages from sender to recipient as read.
     *
     * @param senderId    the sender's ID
     * @param recipientId the recipient's ID
     */
    public void markMessagesAsRead(String senderId, String recipientId) {
        log.debug("Marking messages as read - sender: {}, recipient: {}", senderId, recipientId);
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        chatId.ifPresent(id -> {
            List<ChatMessage> messages = chatMessageRepository.findByChatId(id);
            int count = 0;
            for (ChatMessage msg : messages) {
                if (msg.getRecipientId().equals(recipientId) && msg.getStatus() != MessageStatus.READ) {
                    msg.setStatus(MessageStatus.READ);
                    chatMessageRepository.save(msg);
                    count++;
                }
            }
            log.info("Marked {} messages as READ", count);
        });
    }

    /**
     * Count unread messages for a recipient from a specific sender.
     *
     * @param recipientId the recipient's ID
     * @param senderId    the sender's ID
     * @return count of unread messages
     */
    public long countUnreadMessages(String recipientId, String senderId) {
        return chatMessageRepository.countByRecipientIdAndSenderIdAndStatus(recipientId, senderId, MessageStatus.SENT);
    }

    /**
     * Mark messages as read and return the updated messages.
     *
     * @param senderId    the sender's ID
     * @param recipientId the recipient's ID
     * @return list of messages that were marked as read
     */
    public List<ChatMessage> markMessagesAsReadAndReturn(String senderId, String recipientId) {
        log.debug("Marking messages as read and returning - sender: {}, recipient: {}", senderId, recipientId);
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        List<ChatMessage> readMessages = new ArrayList<>();

        chatId.ifPresent(id -> {
            List<ChatMessage> messages = chatMessageRepository.findByChatId(id);
            messages.stream()
                    .filter(msg -> msg.getSenderId().equals(senderId) &&
                            msg.getRecipientId().equals(recipientId) &&
                            msg.getStatus() != MessageStatus.READ)
                    .forEach(msg -> {
                        msg.setStatus(MessageStatus.READ);
                        msg.setReadTimestamp(new Date());
                        chatMessageRepository.save(msg);
                        readMessages.add(msg);
                    });
        });

        log.info("Marked {} messages as READ", readMessages.size());
        return readMessages;
    }

    /**
     * Get chat contacts for a user with last message info.
     *
     * @param userId the user's ID
     * @return list of chat contacts sorted by last message time
     */
    public List<ChatContactResponse> getChatContacts(String userId) {
        log.debug("Getting chat contacts for user: {}", userId);
        List<String> chatPartners = chatRoomService.getChatPartners(userId);
        List<ChatContactResponse> contacts = new ArrayList<>();

        for (String partnerId : chatPartners) {
            User partner = userService.findByUsername(partnerId).orElse(null);
            if (partner == null) {
                log.warn("Partner user not found: {}", partnerId);
                continue;
            }

            var chatId = chatRoomService.getChatRoomId(userId, partnerId, false);

            if (chatId.isPresent()) {
                var lastMessage = chatMessageRepository.findTopByChatIdOrderByTimeStampDesc(chatId.get());

                long unreadCount = chatMessageRepository.countByRecipientIdAndSenderIdAndStatusNot(
                        userId, partnerId, MessageStatus.READ);

                ChatContactResponse contact = ChatContactResponse.builder()
                        .username(partner.getUsername())
                        .fullName(partner.getFullName())
                        .status(partner.getStatus())
                        .lastMessage(lastMessage.map(ChatMessage::getContent).orElse(null))
                        .lastMessageType(lastMessage
                                .map(m -> m.getMessageType() != null ? m.getMessageType().name() : "TEXT").orElse(null))
                        .lastMessageTime(lastMessage.map(ChatMessage::getTimeStamp).orElse(null))
                        .lastMessageSenderId(lastMessage.map(ChatMessage::getSenderId).orElse(null))
                        .unreadCount(unreadCount)
                        .build();

                contacts.add(contact);
            }
        }

        contacts.sort((c1, c2) -> {
            if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null)
                return 0;
            if (c1.getLastMessageTime() == null)
                return 1;
            if (c2.getLastMessageTime() == null)
                return -1;
            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
        });

        log.debug("Found {} contacts for user {}", contacts.size(), userId);
        return contacts;
    }
}
