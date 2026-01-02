package com.project.ChatNexus.controller;

import com.project.ChatNexus.dto.response.ChatContactResponse;
import com.project.ChatNexus.dto.response.ChatNotification;
import com.project.ChatNexus.model.ChatMessage;
import com.project.ChatNexus.model.MessageStatus;
import com.project.ChatNexus.service.ChatMessageService;
import com.project.ChatNexus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

/**
 * Controller handling chat operations including messaging via WebSocket
 * and REST endpoints for message retrieval.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat messaging operations")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    /**
     * Process incoming chat message via WebSocket.
     * Saves the message and delivers it to the recipient if online.
     *
     * @param chatMessage the message to process
     */
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        log.info("Processing message from {} to {}", chatMessage.getSenderId(), chatMessage.getRecipientId());

        ChatMessage savedMsg = chatMessageService.save(chatMessage);
        log.debug("Message saved with ID: {}", savedMsg.getId());

        // Only send via WebSocket if recipient is online
        if (userService.isUserOnline(chatMessage.getRecipientId())) {
            log.debug("Recipient {} is online, delivering message", chatMessage.getRecipientId());

            // Send message to recipient
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getRecipientId(),
                    "/queue/messages",
                    ChatNotification.builder()
                            .id(savedMsg.getId())
                            .senderId(savedMsg.getSenderId())
                            .recipientId(savedMsg.getRecipientId())
                            .content(savedMsg.getContent())
                            .status(MessageStatus.DELIVERED)
                            .timestamp(savedMsg.getTimeStamp())
                            .messageType(savedMsg.getMessageType())
                            .mediaUrl(savedMsg.getMediaUrl())
                            .fileName(savedMsg.getFileName())
                            .fileSize(savedMsg.getFileSize())
                            .mimeType(savedMsg.getMimeType())
                            .build()
            );

            // Send delivery confirmation back to sender
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSenderId(),
                    "/queue/status",
                    ChatNotification.builder()
                            .id(savedMsg.getId())
                            .senderId(savedMsg.getSenderId())
                            .recipientId(savedMsg.getRecipientId())
                            .status(MessageStatus.DELIVERED)
                            .timestamp(savedMsg.getTimeStamp())
                            .build()
            );

            log.info("Message {} delivered successfully", savedMsg.getId());
        } else {
            log.info("Recipient {} is offline, message {} stored for later delivery",
                    chatMessage.getRecipientId(), savedMsg.getId());
        }
    }

    /**
     * Mark messages as read via WebSocket.
     *
     * @param payload containing senderId and recipientId
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, String> payload) {
        String senderId = payload.get("senderId");
        String recipientId = payload.get("recipientId");

        log.info("Marking messages as read - sender: {}, recipient: {}", senderId, recipientId);

        List<ChatMessage> readMessages = chatMessageService.markMessagesAsReadAndReturn(senderId, recipientId);
        log.debug("Marked {} messages as read", readMessages.size());

        // Send read confirmation to the original sender if online
        if (userService.isUserOnline(senderId)) {
            readMessages.forEach(msg -> {
                messagingTemplate.convertAndSendToUser(
                        senderId,
                        "/queue/status",
                        ChatNotification.builder()
                                .id(msg.getId())
                                .senderId(msg.getSenderId())
                                .recipientId(msg.getRecipientId())
                                .status(MessageStatus.READ)
                                .timestamp(msg.getTimeStamp())
                                .readTimestamp(msg.getReadTimestamp())
                                .build()
                );
            });
            log.debug("Read confirmations sent to {}", senderId);
        }
    }

    @Operation(
            summary = "Get chat messages",
            description = "Retrieve all messages between two users"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ChatMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findChatMessages(
            @Parameter(description = "ID of the sender (current user)") @PathVariable("senderId") String senderId,
            @Parameter(description = "ID of the recipient") @PathVariable("recipientId") String recipientId
    ) {
        log.info("Fetching chat messages between {} and {}", senderId, recipientId);
        List<ChatMessage> messages = chatMessageService.findChatMessages(senderId, recipientId);
        log.debug("Found {} messages", messages.size());
        return ResponseEntity.ok(messages);
    }

    @Operation(
            summary = "Get undelivered messages",
            description = "Retrieve messages that were sent while the user was offline"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Undelivered messages retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/messages/undelivered/{userId}")
    public ResponseEntity<List<ChatMessage>> getUndeliveredMessages(
            @Parameter(description = "ID of the user to get undelivered messages for") @PathVariable String userId
    ) {
        log.info("Fetching undelivered messages for user: {}", userId);
        List<ChatMessage> undeliveredMessages = chatMessageService.findUndeliveredMessages(userId);
        log.debug("Found {} undelivered messages", undeliveredMessages.size());

        // Send delivery confirmation to original senders who are online
        undeliveredMessages.forEach(msg -> {
            if (userService.isUserOnline(msg.getSenderId())) {
                messagingTemplate.convertAndSendToUser(
                        msg.getSenderId(),
                        "/queue/status",
                        ChatNotification.builder()
                                .id(msg.getId())
                                .senderId(msg.getSenderId())
                                .recipientId(msg.getRecipientId())
                                .status(MessageStatus.DELIVERED)
                                .build()
                );
                log.debug("Delivery confirmation sent to {}", msg.getSenderId());
            }
        });

        chatMessageService.markMessagesAsDelivered(undeliveredMessages);
        return ResponseEntity.ok(undeliveredMessages);
    }

    @Operation(
            summary = "Mark messages as read",
            description = "Mark all messages from a sender to recipient as read"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/messages/read/{senderId}/{recipientId}")
    public ResponseEntity<Void> markMessagesAsRead(
            @Parameter(description = "ID of the original sender") @PathVariable String senderId,
            @Parameter(description = "ID of the recipient (current user)") @PathVariable String recipientId
    ) {
        log.info("REST request to mark messages as read - sender: {}, recipient: {}", senderId, recipientId);
        chatMessageService.markMessagesAsRead(senderId, recipientId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get chat contacts",
            description = "Get list of contacts with whom the user has chatted, sorted by last message time"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ChatContactResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/contacts/{userId}")
    public ResponseEntity<List<ChatContactResponse>> getChatContacts(
            @Parameter(description = "ID of the user") @PathVariable String userId
    ) {
        log.info("Fetching chat contacts for user: {}", userId);
        List<ChatContactResponse> contacts = chatMessageService.getChatContacts(userId);
        log.debug("Found {} contacts", contacts.size());
        return ResponseEntity.ok(contacts);
    }
}

