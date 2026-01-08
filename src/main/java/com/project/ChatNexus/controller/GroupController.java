package com.project.ChatNexus.controller;

import com.project.ChatNexus.dto.request.AddMembersRequest;
import com.project.ChatNexus.dto.request.CreateGroupRequest;
import com.project.ChatNexus.dto.response.GroupMessageNotification;
import com.project.ChatNexus.dto.response.GroupResponse;
import com.project.ChatNexus.model.Group;
import com.project.ChatNexus.model.GroupMessage;
import com.project.ChatNexus.model.MessageType;
import com.project.ChatNexus.service.GroupMessageService;
import com.project.ChatNexus.service.GroupService;
import com.project.ChatNexus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Controller handling group chat operations including messaging via WebSocket
 * and REST endpoints for group management.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Groups", description = "Group chat operations")
public class GroupController {

    private final GroupService groupService;
    private final GroupMessageService groupMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    /**
     * Process incoming group message via WebSocket.
     * Saves the message and broadcasts to all group members.
     */
    @MessageMapping("/group.chat")
    public void processGroupMessage(@Payload GroupMessage groupMessage) {
        log.info("Processing group message from {} to group {}",
                groupMessage.getSenderId(), groupMessage.getGroupId());

        GroupMessage savedMsg = groupMessageService.save(groupMessage);
        log.debug("Group message saved with ID: {}", savedMsg.getId());

        // Get group to find all members
        groupService.getGroupById(groupMessage.getGroupId()).ifPresent(group -> {
            GroupMessageNotification notification = GroupMessageNotification.builder()
                    .id(savedMsg.getId())
                    .groupId(savedMsg.getGroupId())
                    .groupName(group.getName())
                    .senderId(savedMsg.getSenderId())
                    .senderName(savedMsg.getSenderName())
                    .content(savedMsg.getContent())
                    .timestamp(savedMsg.getTimestamp())
                    .messageType(savedMsg.getMessageType())
                    .mediaUrl(savedMsg.getMediaUrl())
                    .fileName(savedMsg.getFileName())
                    .fileSize(savedMsg.getFileSize())
                    .mimeType(savedMsg.getMimeType())
                    .build();

            // Send to all group members via their personal queues
            for (String memberId : group.getMemberIds()) {
                if (userService.isUserOnline(memberId)) {
                    messagingTemplate.convertAndSendToUser(
                            memberId,
                            "/queue/group-messages",
                            notification);
                    log.debug("Sent group message to member: {}", memberId);
                }
            }

            log.info("Group message {} broadcast to {} members",
                    savedMsg.getId(), group.getMemberIds().size());
        });
    }

    /**
     * Create a new group.
     */
    @Operation(summary = "Create a new group")
    @PostMapping("/groups")
    @ResponseBody
    public ResponseEntity<GroupResponse> createGroup(
            @RequestParam String creatorId,
            @Valid @RequestBody CreateGroupRequest request) {

        log.info("Creating group '{}' by user {}", request.getName(), creatorId);
        Group group = groupService.createGroup(creatorId, request);

        // Get creator's name
        String creatorName = userService.findByUsername(creatorId)
                .map(u -> u.getFullName())
                .orElse(creatorId);

        // Create system message for group creation
        GroupMessage systemMsg = GroupMessage.builder()
                .groupId(group.getId())
                .senderId("SYSTEM")
                .senderName("System")
                .content(creatorName + " created the group \"" + group.getName() + "\"")
                .timestamp(new Date())
                .messageType(MessageType.SYSTEM)
                .build();
        GroupMessage savedSystemMsg = groupMessageService.save(systemMsg);

        // Build notification for the creation system message
        GroupMessageNotification creationNotification = GroupMessageNotification.builder()
                .id(savedSystemMsg.getId())
                .groupId(savedSystemMsg.getGroupId())
                .groupName(group.getName())
                .senderId(savedSystemMsg.getSenderId())
                .senderName(savedSystemMsg.getSenderName())
                .content(savedSystemMsg.getContent())
                .timestamp(savedSystemMsg.getTimestamp())
                .messageType(savedSystemMsg.getMessageType())
                .build();

        // Broadcast creation system message to all members
        for (String memberId : group.getMemberIds()) {
            if (userService.isUserOnline(memberId)) {
                messagingTemplate.convertAndSendToUser(
                        memberId,
                        "/queue/group-messages",
                        creationNotification);
            }
        }

        // If there are other members (besides creator), add system messages for them
        // being added
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (String memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    String memberName = userService.findByUsername(memberId)
                            .map(u -> u.getFullName())
                            .orElse(memberId);

                    GroupMessage addedMsg = GroupMessage.builder()
                            .groupId(group.getId())
                            .senderId("SYSTEM")
                            .senderName("System")
                            .content(memberName + " was added to the group")
                            .timestamp(new Date())
                            .messageType(MessageType.SYSTEM)
                            .build();
                    GroupMessage savedAddedMsg = groupMessageService.save(addedMsg);

                    // Broadcast added system message
                    GroupMessageNotification addedNotification = GroupMessageNotification.builder()
                            .id(savedAddedMsg.getId())
                            .groupId(savedAddedMsg.getGroupId())
                            .groupName(group.getName())
                            .senderId(savedAddedMsg.getSenderId())
                            .senderName(savedAddedMsg.getSenderName())
                            .content(savedAddedMsg.getContent())
                            .timestamp(savedAddedMsg.getTimestamp())
                            .messageType(savedAddedMsg.getMessageType())
                            .build();

                    for (String existingMember : group.getMemberIds()) {
                        if (userService.isUserOnline(existingMember)) {
                            messagingTemplate.convertAndSendToUser(
                                    existingMember,
                                    "/queue/group-messages",
                                    addedNotification);
                        }
                    }
                }
            }
        }

        // Notify all initial members about the new group (for sidebar update)
        GroupResponse response = mapToGroupResponse(group);
        for (String memberId : group.getMemberIds()) {
            if (userService.isUserOnline(memberId)) {
                messagingTemplate.convertAndSendToUser(
                        memberId,
                        "/queue/group-updates",
                        Map.of("type", "GROUP_CREATED", "group", response));
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get all groups for a user.
     */
    @Operation(summary = "Get user's groups")
    @GetMapping("/groups/user/{userId}")
    @ResponseBody
    public ResponseEntity<List<GroupResponse>> getUserGroups(@PathVariable String userId) {
        log.debug("Fetching groups for user: {}", userId);
        List<GroupResponse> groups = groupService.getGroupsForUser(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * Get group by ID.
     */
    @Operation(summary = "Get group details")
    @GetMapping("/groups/{groupId}")
    @ResponseBody
    public ResponseEntity<GroupResponse> getGroup(@PathVariable String groupId) {
        return groupService.getGroupById(groupId)
                .map(group -> ResponseEntity.ok(mapToGroupResponse(group)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get group messages and mark as read.
     */
    @Operation(summary = "Get group messages")
    @GetMapping("/groups/{groupId}/messages")
    @ResponseBody
    public ResponseEntity<List<GroupMessage>> getGroupMessages(
            @PathVariable String groupId,
            @RequestParam String userId) {

        log.debug("Fetching messages for group {} by user {}", groupId, userId);
        List<GroupMessage> messages = groupMessageService.findGroupMessages(groupId, userId);

        // Mark group as read when user fetches messages
        groupService.markGroupAsRead(groupId, userId);

        return ResponseEntity.ok(messages);
    }

    /**
     * Mark group as read for a user.
     */
    @Operation(summary = "Mark group as read")
    @PostMapping("/groups/{groupId}/read")
    @ResponseBody
    public ResponseEntity<?> markGroupAsRead(
            @PathVariable String groupId,
            @RequestParam String userId) {

        log.debug("Marking group {} as read for user {}", groupId, userId);
        groupService.markGroupAsRead(groupId, userId);
        return ResponseEntity.ok(Map.of("message", "Group marked as read"));
    }

    /**
     * Add members to a group.
     */
    @Operation(summary = "Add members to group")
    @PostMapping("/groups/{groupId}/members")
    @ResponseBody
    public ResponseEntity<GroupResponse> addMembers(
            @PathVariable String groupId,
            @RequestParam String requesterId,
            @Valid @RequestBody AddMembersRequest request) {

        log.info("Adding members to group {} by {}", groupId, requesterId);
        Group group = groupService.addMembers(groupId, requesterId, request.getMemberIds());

        GroupResponse response = mapToGroupResponse(group);

        // Get requester's name for the system message
        String requesterName = userService.findByUsername(requesterId)
                .map(u -> u.getFullName())
                .orElse(requesterId);

        // Create system messages for each added member
        for (String memberId : request.getMemberIds()) {
            String memberName = userService.findByUsername(memberId)
                    .map(u -> u.getFullName())
                    .orElse(memberId);

            // Create system message: "User X was added to the group"
            GroupMessage systemMsg = GroupMessage.builder()
                    .groupId(groupId)
                    .senderId("SYSTEM")
                    .senderName("System")
                    .content(memberName + " was added to the group by " + requesterName)
                    .timestamp(new Date())
                    .messageType(MessageType.SYSTEM)
                    .build();

            GroupMessage savedMsg = groupMessageService.save(systemMsg);

            // Create notification for the system message
            GroupMessageNotification systemNotification = GroupMessageNotification.builder()
                    .id(savedMsg.getId())
                    .groupId(savedMsg.getGroupId())
                    .groupName(group.getName())
                    .senderId(savedMsg.getSenderId())
                    .senderName(savedMsg.getSenderName())
                    .content(savedMsg.getContent())
                    .timestamp(savedMsg.getTimestamp())
                    .messageType(savedMsg.getMessageType())
                    .build();

            // Send system message to all online members
            for (String existingMember : group.getMemberIds()) {
                if (userService.isUserOnline(existingMember)) {
                    messagingTemplate.convertAndSendToUser(
                            existingMember,
                            "/queue/group-messages",
                            systemNotification);
                }
            }

            // Notify the added member specifically
            if (userService.isUserOnline(memberId)) {
                messagingTemplate.convertAndSendToUser(
                        memberId,
                        "/queue/group-updates",
                        Map.of("type", "ADDED_TO_GROUP", "group", response));
            }
        }

        // Notify existing members about new members
        for (String memberId : group.getMemberIds()) {
            if (!request.getMemberIds().contains(memberId) && userService.isUserOnline(memberId)) {
                messagingTemplate.convertAndSendToUser(
                        memberId,
                        "/queue/group-updates",
                        Map.of("type", "MEMBERS_ADDED", "group", response));
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a member from a group.
     */
    @Operation(summary = "Remove member from group")
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    @ResponseBody
    public ResponseEntity<GroupResponse> removeMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            @RequestParam String requesterId) {

        log.info("Removing member {} from group {} by {}", memberId, groupId, requesterId);
        Group group = groupService.removeMember(groupId, requesterId, memberId);

        // Notify removed member
        if (userService.isUserOnline(memberId)) {
            messagingTemplate.convertAndSendToUser(
                    memberId,
                    "/queue/group-updates",
                    Map.of("type", "REMOVED_FROM_GROUP", "groupId", groupId));
        }

        return ResponseEntity.ok(mapToGroupResponse(group));
    }

    /**
     * Leave a group.
     */
    @Operation(summary = "Leave a group")
    @PostMapping("/groups/{groupId}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveGroup(
            @PathVariable String groupId,
            @RequestParam String userId) {

        log.info("User {} leaving group {}", userId, groupId);

        // Get user's name and group info before leaving
        String userName = userService.findByUsername(userId)
                .map(u -> u.getFullName())
                .orElse(userId);

        Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        String groupName = group.getName();

        // Leave the group
        groupService.leaveGroup(groupId, userId);

        // Check if group still exists (not deleted due to empty members)
        if (groupService.getGroupById(groupId).isPresent()) {
            // Create and broadcast system message about user leaving
            GroupMessage systemMsg = GroupMessage.builder()
                    .groupId(groupId)
                    .senderId("SYSTEM")
                    .senderName("System")
                    .content(userName + " left the group")
                    .timestamp(new Date())
                    .messageType(MessageType.SYSTEM)
                    .build();
            GroupMessage savedMsg = groupMessageService.save(systemMsg);

            // Broadcast to remaining members
            Group updatedGroup = groupService.getGroupById(groupId).get();
            GroupMessageNotification notification = GroupMessageNotification.builder()
                    .id(savedMsg.getId())
                    .groupId(savedMsg.getGroupId())
                    .groupName(groupName)
                    .senderId(savedMsg.getSenderId())
                    .senderName(savedMsg.getSenderName())
                    .content(savedMsg.getContent())
                    .timestamp(savedMsg.getTimestamp())
                    .messageType(savedMsg.getMessageType())
                    .build();

            for (String memberId : updatedGroup.getMemberIds()) {
                if (userService.isUserOnline(memberId)) {
                    messagingTemplate.convertAndSendToUser(
                            memberId,
                            "/queue/group-messages",
                            notification);
                    // Also notify about group update for sidebar
                    messagingTemplate.convertAndSendToUser(
                            memberId,
                            "/queue/group-updates",
                            Map.of("type", "MEMBER_LEFT", "group", mapToGroupResponse(updatedGroup)));
                }
            }
        }

        return ResponseEntity.ok(Map.of("message", "Successfully left the group"));
    }

    /**
     * Get group members.
     */
    @Operation(summary = "Get group members")
    @GetMapping("/groups/{groupId}/members")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getGroupMembers(@PathVariable String groupId) {
        log.debug("Fetching members for group {}", groupId);
        List<Map<String, Object>> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    /**
     * Update group details.
     */
    @Operation(summary = "Update group details")
    @PutMapping("/groups/{groupId}")
    @ResponseBody
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable String groupId,
            @RequestParam String requesterId,
            @RequestBody Map<String, String> updates) {

        log.info("Updating group {} by {}", groupId, requesterId);
        Group group = groupService.updateGroup(
                groupId,
                requesterId,
                updates.get("name"),
                updates.get("description"));

        // Notify all members about the update
        GroupResponse response = mapToGroupResponse(group);
        for (String memberId : group.getMemberIds()) {
            if (userService.isUserOnline(memberId)) {
                messagingTemplate.convertAndSendToUser(
                        memberId,
                        "/queue/group-updates",
                        Map.of("type", "GROUP_UPDATED", "group", response));
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a group.
     */
    @Operation(summary = "Delete a group")
    @DeleteMapping("/groups/{groupId}")
    @ResponseBody
    public ResponseEntity<?> deleteGroup(
            @PathVariable String groupId,
            @RequestParam String requesterId) {

        log.info("Deleting group {} by {}", groupId, requesterId);

        // Get members before deletion to notify them
        groupService.getGroupById(groupId).ifPresent(group -> {
            for (String memberId : group.getMemberIds()) {
                if (userService.isUserOnline(memberId)) {
                    messagingTemplate.convertAndSendToUser(
                            memberId,
                            "/queue/group-updates",
                            Map.of("type", "GROUP_DELETED", "groupId", groupId));
                }
            }
        });

        groupService.deleteGroup(groupId, requesterId);

        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }

    private GroupResponse mapToGroupResponse(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .creatorId(group.getCreatorId())
                .memberIds(group.getMemberIds())
                .adminIds(group.getAdminIds())
                .avatarUrl(group.getAvatarUrl())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .memberCount(group.getMemberIds() != null ? group.getMemberIds().size() : 0)
                .build();
    }
}
