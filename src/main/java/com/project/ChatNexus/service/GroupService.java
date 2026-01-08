package com.project.ChatNexus.service;

import com.project.ChatNexus.dto.request.CreateGroupRequest;
import com.project.ChatNexus.dto.response.GroupResponse;
import com.project.ChatNexus.model.Group;
import com.project.ChatNexus.model.GroupMessage;
import com.project.ChatNexus.model.GroupReadStatus;
import com.project.ChatNexus.model.MessageType;
import com.project.ChatNexus.repository.GroupMessageRepository;
import com.project.ChatNexus.repository.GroupReadStatusRepository;
import com.project.ChatNexus.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupReadStatusRepository groupReadStatusRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    /**
     * Create a new group.
     */
    public Group createGroup(String creatorId, CreateGroupRequest request) {
        log.info("Creating group '{}' by user {}", request.getName(), creatorId);

        Set<String> members = new HashSet<>();
        members.add(creatorId); // Creator is always a member

        if (request.getMemberIds() != null) {
            members.addAll(request.getMemberIds());
        }

        Set<String> admins = new HashSet<>();
        admins.add(creatorId); // Creator is always an admin

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creatorId(creatorId)
                .memberIds(members)
                .adminIds(admins)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Group savedGroup = groupRepository.save(group);
        log.info("Group created with ID: {}", savedGroup.getId());
        return savedGroup;
    }

    /**
     * Get a group by ID.
     */
    public Optional<Group> getGroupById(String groupId) {
        return groupRepository.findById(groupId);
    }

    /**
     * Get all groups for a user.
     */
    public List<GroupResponse> getGroupsForUser(String userId) {
        log.debug("Fetching groups for user: {}", userId);
        List<Group> groups = groupRepository.findByMemberIdsContaining(userId);

        return groups.stream()
                .map(group -> mapToGroupResponseWithUnread(group, userId))
                .sorted((g1, g2) -> {
                    if (g1.getLastMessageTime() == null && g2.getLastMessageTime() == null)
                        return 0;
                    if (g1.getLastMessageTime() == null)
                        return 1;
                    if (g2.getLastMessageTime() == null)
                        return -1;
                    return g2.getLastMessageTime().compareTo(g1.getLastMessageTime());
                })
                .toList();
    }

    /**
     * Add members to a group.
     */
    public Group addMembers(String groupId, String requesterId, Set<String> memberIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.isMember(requesterId)) {
            throw new RuntimeException("You are not a member of this group");
        }

        memberIds.forEach(group::addMember);
        group.setUpdatedAt(LocalDateTime.now());

        log.info("Added {} members to group {}", memberIds.size(), groupId);
        return groupRepository.save(group);
    }

    /**
     * Remove a member from a group.
     */
    public Group removeMember(String groupId, String requesterId, String memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Only admins can remove others, or users can remove themselves
        if (!group.isAdmin(requesterId) && !requesterId.equals(memberId)) {
            throw new RuntimeException("You don't have permission to remove this member");
        }

        group.removeMember(memberId);
        group.setUpdatedAt(LocalDateTime.now());

        log.info("Removed member {} from group {}", memberId, groupId);
        return groupRepository.save(group);
    }

    /**
     * Leave a group.
     */
    public void leaveGroup(String groupId, String userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.removeMember(userId);
        group.setUpdatedAt(LocalDateTime.now());

        // If group is empty, delete the group and all associated data
        if (group.getMemberIds().isEmpty()) {
            deleteGroupCompletely(groupId);
            log.info("Group {} deleted as last member left", groupId);
        } else {
            groupRepository.save(group);
            log.info("User {} left group {}", userId, groupId);
        }
    }

    /**
     * Completely delete a group including all messages, media, and read status.
     */
    private void deleteGroupCompletely(String groupId) {
        // Get all messages to delete media from Cloudinary
        List<GroupMessage> messages = groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);

        // Delete media from Cloudinary for each message with media
        for (GroupMessage message : messages) {
            if (message.getMediaPublicId() != null && !message.getMediaPublicId().isEmpty()) {
                try {
                    String resourceType = getCloudinaryResourceType(message.getMessageType());
                    cloudinaryService.deleteFile(message.getMediaPublicId(), resourceType);
                    log.debug("Deleted media {} from Cloudinary", message.getMediaPublicId());
                } catch (IOException e) {
                    log.error("Failed to delete media {} from Cloudinary: {}",
                            message.getMediaPublicId(), e.getMessage());
                }
            }
        }

        // Delete all messages
        groupMessageRepository.deleteAll(messages);
        log.debug("Deleted {} messages from group {}", messages.size(), groupId);

        // Delete read status records
        groupReadStatusRepository.deleteByGroupId(groupId);
        log.debug("Deleted read status records for group {}", groupId);

        // Delete the group itself
        groupRepository.deleteById(groupId);
    }

    /**
     * Get Cloudinary resource type based on message type.
     */
    private String getCloudinaryResourceType(MessageType messageType) {
        if (messageType == null)
            return "auto";
        return switch (messageType) {
            case IMAGE -> "image";
            case VIDEO, AUDIO -> "video";
            default -> "auto";
        };
    }

    /**
     * Update group details.
     */
    public Group updateGroup(String groupId, String requesterId, String name, String description) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.isAdmin(requesterId)) {
            throw new RuntimeException("Only admins can update group details");
        }

        if (name != null && !name.trim().isEmpty()) {
            group.setName(name.trim());
        }
        if (description != null) {
            group.setDescription(description.trim());
        }
        group.setUpdatedAt(LocalDateTime.now());

        return groupRepository.save(group);
    }

    /**
     * Delete a group.
     */
    public void deleteGroup(String groupId, String requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreatorId().equals(requesterId)) {
            throw new RuntimeException("Only the group creator can delete the group");
        }

        groupRepository.delete(group);
        log.info("Group {} deleted by creator {}", groupId, requesterId);
    }

    /**
     * Get group members with their details.
     */
    public List<Map<String, Object>> getGroupMembers(String groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<Map<String, Object>> members = new ArrayList<>();
        for (String memberId : group.getMemberIds()) {
            userService.findByUsername(memberId).ifPresent(user -> {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("username", user.getUsername());
                memberInfo.put("fullName", user.getFullName());
                memberInfo.put("status", user.getStatus());
                memberInfo.put("isAdmin", group.isAdmin(user.getUsername()));
                memberInfo.put("isCreator", group.getCreatorId().equals(user.getUsername()));
                members.add(memberInfo);
            });
        }
        return members;
    }

    /**
     * Mark group as read for a user (update last read timestamp).
     */
    public void markGroupAsRead(String groupId, String userId) {
        GroupReadStatus readStatus = groupReadStatusRepository
                .findByUserIdAndGroupId(userId, groupId)
                .orElse(GroupReadStatus.builder()
                        .userId(userId)
                        .groupId(groupId)
                        .build());

        readStatus.setLastReadTimestamp(new Date());
        groupReadStatusRepository.save(readStatus);
        log.debug("Marked group {} as read for user {}", groupId, userId);
    }

    /**
     * Get unread message count for a user in a group.
     */
    public int getUnreadCount(String groupId, String userId) {
        Optional<GroupReadStatus> readStatus = groupReadStatusRepository
                .findByUserIdAndGroupId(userId, groupId);

        if (readStatus.isEmpty()) {
            // User has never read this group - count all messages except their own
            return (int) groupMessageRepository.countByGroupId(groupId);
        }

        Date lastReadTime = readStatus.get().getLastReadTimestamp();
        // Count messages after last read, excluding user's own messages
        return (int) groupMessageRepository.countByGroupIdAndTimestampAfterAndSenderIdNot(
                groupId, lastReadTime, userId);
    }

    /**
     * Map Group entity to GroupResponse DTO.
     */
    private GroupResponse mapToGroupResponse(Group group) {
        return mapToGroupResponseWithUnread(group, null);
    }

    /**
     * Map Group entity to GroupResponse DTO with unread count for specific user.
     */
    private GroupResponse mapToGroupResponseWithUnread(Group group, String userId) {
        GroupResponse.GroupResponseBuilder builder = GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .creatorId(group.getCreatorId())
                .memberIds(group.getMemberIds())
                .adminIds(group.getAdminIds())
                .avatarUrl(group.getAvatarUrl())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .memberCount(group.getMemberIds() != null ? group.getMemberIds().size() : 0);

        // Get last message info
        groupMessageRepository.findTopByGroupIdOrderByTimestampDesc(group.getId())
                .ifPresent(lastMsg -> {
                    builder.lastMessage(lastMsg.getContent())
                            .lastMessageSender(lastMsg.getSenderName())
                            .lastMessageTime(lastMsg.getTimestamp())
                            .lastMessageType(lastMsg.getMessageType());
                });

        // Calculate unread count if userId is provided
        if (userId != null) {
            builder.unreadCount(getUnreadCount(group.getId(), userId));
        }

        return builder.build();
    }
}
