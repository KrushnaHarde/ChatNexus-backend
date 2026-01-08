package com.project.ChatNexus.dto.response;

import com.project.ChatNexus.model.MessageType;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageNotification {
    private String id;
    private String groupId;
    private String groupName;
    private String senderId;
    private String senderName;
    private String content;
    private Date timestamp;

    // Media fields
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    private String mediaUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
}
