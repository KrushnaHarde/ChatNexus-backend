package com.project.ChatNexus.service;

import com.project.ChatNexus.dto.response.ChatContactResponse;
import com.project.ChatNexus.model.ChatMessage;
import com.project.ChatNexus.model.MessageStatus;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    public ChatMessage save(ChatMessage chatMessage) {
        var chatId = chatRoomService.getChatRoomId(
                chatMessage.getSenderId(),
                chatMessage.getRecipientId(),
                true
        ).orElseThrow();

        chatMessage.setChatId(chatId);
        chatMessage.setTimeStamp(new Date());

        if (userService.isUserOnline(chatMessage.getRecipientId())) {
            chatMessage.setStatus(MessageStatus.DELIVERED);
        } else {
            chatMessage.setStatus(MessageStatus.SENT);
        }

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        return chatId.map(chatMessageRepository::findByChatId).orElse(new ArrayList<>());
    }

    public List<ChatMessage> findUndeliveredMessages(String recipientId) {
        return chatMessageRepository.findByRecipientIdAndStatus(recipientId, MessageStatus.SENT);
    }

    public void markMessagesAsDelivered(List<ChatMessage> messages) {
        messages.forEach(msg -> {
            msg.setStatus(MessageStatus.DELIVERED);
            chatMessageRepository.save(msg);
        });
    }

    public void markMessageAsDelivered(String messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus(MessageStatus.DELIVERED);
            chatMessageRepository.save(msg);
        });
    }

    public void markMessagesAsRead(String senderId, String recipientId) {
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        chatId.ifPresent(id -> {
            List<ChatMessage> messages = chatMessageRepository.findByChatId(id);
            messages.stream()
                    .filter(msg -> msg.getRecipientId().equals(recipientId) && msg.getStatus() != MessageStatus.READ)
                    .forEach(msg -> {
                        msg.setStatus(MessageStatus.READ);
                        chatMessageRepository.save(msg);
                    });
        });
    }

    public long countUnreadMessages(String recipientId, String senderId) {
        return chatMessageRepository.countByRecipientIdAndSenderIdAndStatus(recipientId, senderId, MessageStatus.SENT);
    }

    public List<ChatMessage> markMessagesAsReadAndReturn(String senderId, String recipientId) {
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

        return readMessages;
    }

    public List<ChatContactResponse> getChatContacts(String userId) {
        List<String> chatPartners = chatRoomService.getChatPartners(userId);
        List<ChatContactResponse> contacts = new ArrayList<>();

        for (String partnerId : chatPartners) {
            User partner = userService.findByUsername(partnerId).orElse(null);
            if (partner == null) continue;

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
                        .lastMessageTime(lastMessage.map(ChatMessage::getTimeStamp).orElse(null))
                        .lastMessageSenderId(lastMessage.map(ChatMessage::getSenderId).orElse(null))
                        .unreadCount(unreadCount)
                        .build();

                contacts.add(contact);
            }
        }

        contacts.sort((c1, c2) -> {
            if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
            if (c1.getLastMessageTime() == null) return 1;
            if (c2.getLastMessageTime() == null) return -1;
            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
        });

        return contacts;
    }
}
