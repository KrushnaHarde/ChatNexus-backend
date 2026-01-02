package com.project.ChatNexus.service;

import com.project.ChatNexus.model.ChatRoom;
import com.project.ChatNexus.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling chat room operations including creation and lookup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * Get or create a chat room ID for two users.
     *
     * @param senderId the sender's ID
     * @param recipientId the recipient's ID
     * @param createNewRoomIfNotExists whether to create a new room if one doesn't exist
     * @return optional containing the chat room ID
     */
    public Optional<String> getChatRoomId(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
        log.debug("Getting chat room for {} and {} (create: {})", senderId, recipientId, createNewRoomIfNotExists);

        return chatRoomRepository
                .findBySenderIdAndRecipientId(senderId, recipientId)
                .map(ChatRoom::getChatId)
                .or(() -> {
                    if (createNewRoomIfNotExists) {
                        var chatId = createChatId(senderId, recipientId);
                        log.info("Created new chat room: {} for {} and {}", chatId, senderId, recipientId);
                        return Optional.of(chatId);
                    }
                    log.debug("Chat room not found for {} and {}", senderId, recipientId);
                    return Optional.empty();
                });
    }

    /**
     * Get all chat partners for a user.
     *
     * @param userId the user's ID
     * @return list of recipient IDs
     */
    public List<String> getChatPartners(String userId) {
        log.debug("Getting chat partners for user: {}", userId);
        List<String> partners = chatRoomRepository.findBySenderId(userId)
                .stream()
                .map(ChatRoom::getRecipientId)
                .collect(Collectors.toList());
        log.debug("Found {} chat partners for user {}", partners.size(), userId);
        return partners;
    }

    /**
     * Create a new chat room between two users.
     *
     * @param senderId the sender's ID
     * @param recipientId the recipient's ID
     * @return the new chat room ID
     */
    private String createChatId(String senderId, String recipientId) {
        var chatId = String.format("%s_%s", senderId, recipientId);
        log.debug("Creating chat room with ID: {}", chatId);

        ChatRoom senderRecipient = ChatRoom.builder()
                .chatId(chatId)
                .senderId(senderId)
                .recipientId(recipientId)
                .build();

        ChatRoom recipientSender = ChatRoom.builder()
                .chatId(chatId)
                .senderId(recipientId)
                .recipientId(senderId)
                .build();

        chatRoomRepository.save(senderRecipient);
        chatRoomRepository.save(recipientSender);

        log.debug("Chat room {} created successfully", chatId);
        return chatId;
    }
}
