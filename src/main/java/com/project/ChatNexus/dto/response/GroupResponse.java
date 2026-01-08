package com.project.ChatNexus.dto.response;

import com.project.ChatNexus.model.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String creatorId;
    private Set<String> memberIds;
    private Set<String> adminIds;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Last message info for sidebar
    private String lastMessage;
    private String lastMessageSender;
    private Date lastMessageTime;
    private MessageType lastMessageType;
    private int memberCount;

    // Unread count for the requesting user
    @Builder.Default
    private int unreadCount = 0;
}
