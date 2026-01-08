package com.project.ChatNexus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "groups")
public class Group {
    @Id
    private String id;

    private String name;
    private String description;
    private String creatorId;

    @Builder.Default
    private Set<String> memberIds = new HashSet<>();

    @Builder.Default
    private Set<String> adminIds = new HashSet<>();

    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void addMember(String memberId) {
        if (memberIds == null) {
            memberIds = new HashSet<>();
        }
        memberIds.add(memberId);
    }

    public void removeMember(String memberId) {
        if (memberIds != null) {
            memberIds.remove(memberId);
        }
    }

    public boolean isMember(String userId) {
        return memberIds != null && memberIds.contains(userId);
    }

    public void addAdmin(String adminId) {
        if (adminIds == null) {
            adminIds = new HashSet<>();
        }
        adminIds.add(adminId);
    }

    public boolean isAdmin(String userId) {
        return adminIds != null && adminIds.contains(userId);
    }
}
