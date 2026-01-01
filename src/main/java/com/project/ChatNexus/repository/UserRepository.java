package com.project.ChatNexus.repository;

import com.project.ChatNexus.model.Status;
import com.project.ChatNexus.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    List<User> findAllByStatus(Status status);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String username);
}

