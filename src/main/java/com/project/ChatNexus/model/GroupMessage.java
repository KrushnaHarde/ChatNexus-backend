package com.project.ChatNexus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "group_messages")
public class GroupMessage {
    @Id
    private String id;

    private String groupId;
    private String senderId;
    private String senderName;
    private String content;
    private Date timestamp;

    // Media fields
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    private String mediaUrl;
    private String mediaPublicId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
}
