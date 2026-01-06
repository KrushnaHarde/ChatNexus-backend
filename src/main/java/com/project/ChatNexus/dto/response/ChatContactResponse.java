package com.project.ChatNexus.dto.response;

import com.project.ChatNexus.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContactResponse {
    private String username;
    private String fullName;
    private Status status;
    private String lastMessage;
    private String lastMessageType;
    private Date lastMessageTime;
    private long unreadCount;
    private String lastMessageSenderId;
}
