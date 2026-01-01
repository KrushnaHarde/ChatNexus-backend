package com.project.ChatNexus.repository;

import com.project.ChatNexus.model.ChatMessage;
import com.project.ChatNexus.model.MessageStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByChatId(String chatId);

    List<ChatMessage> findByRecipientIdAndStatus(String recipientId, MessageStatus status);

    long countByRecipientIdAndSenderIdAndStatus(String recipientId, String senderId, MessageStatus status);

    Optional<ChatMessage> findTopByChatIdOrderByTimeStampDesc(String chatId);

    long countByRecipientIdAndSenderIdAndStatusNot(String recipientId, String senderId, MessageStatus status);
}
