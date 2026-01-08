package com.project.ChatNexus.repository;

import com.project.ChatNexus.model.GroupReadStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupReadStatusRepository extends MongoRepository<GroupReadStatus, String> {

    Optional<GroupReadStatus> findByUserIdAndGroupId(String userId, String groupId);

    void deleteByGroupId(String groupId);

    void deleteByUserIdAndGroupId(String userId, String groupId);
}
