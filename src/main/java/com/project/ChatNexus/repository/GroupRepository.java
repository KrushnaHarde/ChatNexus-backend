package com.project.ChatNexus.repository;

import com.project.ChatNexus.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {

    List<Group> findByMemberIdsContaining(String memberId);

    List<Group> findByCreatorId(String creatorId);

    List<Group> findByNameContainingIgnoreCase(String name);
}
