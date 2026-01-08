package com.project.ChatNexus.service;

import com.project.ChatNexus.model.GroupMessage;
import com.project.ChatNexus.model.Group;
import com.project.ChatNexus.model.MessageType;
import com.project.ChatNexus.repository.GroupMessageRepository;
import com.project.ChatNexus.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupRepository groupRepository;
    private final UserService userService;

    /**
     * Save a group message.
     */
    public GroupMessage save(GroupMessage message) {
        log.debug("Saving group message from {} to group {}", message.getSenderId(), message.getGroupId());

        // Verify group exists
        Group group = groupRepository.findById(message.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Allow SYSTEM messages without membership check
        boolean isSystemMessage = "SYSTEM".equals(message.getSenderId()) ||
                message.getMessageType() == MessageType.SYSTEM;

        if (!isSystemMessage && !group.isMember(message.getSenderId())) {
            throw new RuntimeException("User is not a member of this group");
        }

        // Set sender name if not already set (skip for system messages)
        if (!isSystemMessage && (message.getSenderName() == null || message.getSenderName().isEmpty())) {
            userService.findByUsername(message.getSenderId())
                    .ifPresent(user -> message.setSenderName(user.getFullName()));
        }

        if (message.getTimestamp() == null) {
            message.setTimestamp(new Date());
        }

        GroupMessage savedMessage = groupMessageRepository.save(message);
        log.info("Group message saved with ID: {}", savedMessage.getId());
        return savedMessage;
    }

    /**
     * Find all messages in a group.
     */
    public List<GroupMessage> findGroupMessages(String groupId, String requesterId) {
        log.debug("Finding messages for group {}", groupId);

        // Verify user is a member of the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.isMember(requesterId)) {
            throw new RuntimeException("User is not a member of this group");
        }

        List<GroupMessage> messages = groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
        log.debug("Found {} messages in group {}", messages.size(), groupId);
        return messages;
    }

    /**
     * Get the last message in a group.
     */
    public GroupMessage getLastMessage(String groupId) {
        return groupMessageRepository.findTopByGroupIdOrderByTimestampDesc(groupId)
                .orElse(null);
    }
}
