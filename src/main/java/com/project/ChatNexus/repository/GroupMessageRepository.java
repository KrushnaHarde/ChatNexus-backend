package com.project.ChatNexus.repository;

import com.project.ChatNexus.model.GroupMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMessageRepository extends MongoRepository<GroupMessage, String> {

    List<GroupMessage> findByGroupIdOrderByTimestampAsc(String groupId);

    Optional<GroupMessage> findTopByGroupIdOrderByTimestampDesc(String groupId);

    long countByGroupId(String groupId);

    // Count messages after a certain timestamp (for unread count)
    long countByGroupIdAndTimestampAfter(String groupId, Date timestamp);

    // Count messages after timestamp excluding sender's own messages
    long countByGroupIdAndTimestampAfterAndSenderIdNot(String groupId, Date timestamp, String senderId);
}
