package com.project.ChatNexus.service;

import com.project.ChatNexus.model.Status;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service handling user operations including presence management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Save or update user status to ONLINE.
     *
     * @param user the user to save/update
     */
    public void saveUser(User user) {
        log.debug("Setting user {} to ONLINE", user.getUsername());
        var storedUser = userRepository.findByUsername(user.getUsername())
                .orElse(null);

        if (storedUser != null) {
            storedUser.setStatus(Status.ONLINE);
            storedUser.setLastSeen(LocalDateTime.now());
            userRepository.save(storedUser);
            log.info("User {} is now ONLINE", user.getUsername());
        }
    }

    /**
     * Set user status to OFFLINE.
     *
     * @param user the user to disconnect
     */
    public void disconnect(User user) {
        log.debug("Setting user {} to OFFLINE", user.getUsername());
        var storedUser = userRepository.findByUsername(user.getUsername())
                .orElse(null);

        if (storedUser != null) {
            storedUser.setStatus(Status.OFFLINE);
            storedUser.setLastSeen(LocalDateTime.now());
            userRepository.save(storedUser);
            log.info("User {} is now OFFLINE", user.getUsername());
        }
    }

    /**
     * Find all currently connected (online) users.
     *
     * @return list of online users
     */
    public List<User> findConnectedUsers() {
        log.debug("Fetching all connected users");
        List<User> users = userRepository.findAllByStatus(Status.ONLINE);
        log.debug("Found {} connected users", users.size());
        return users;
    }

    /**
     * Find all registered users.
     *
     * @return list of all users
     */
    public List<User> findAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Find user by username.
     *
     * @param username the username to find
     * @return optional containing the user if found
     */
    public Optional<User> findByUsername(String username) {
        log.debug("Looking up user: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Search users by username (partial match).
     *
     * @param query the search query
     * @return list of matching users
     */
    public List<User> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);
        log.debug("Search returned {} users", users.size());
        return users;
    }

    /**
     * Check if a user is currently online.
     *
     * @param username the username to check
     * @return true if user is online, false otherwise
     */
    public boolean isUserOnline(String username) {
        boolean online = userRepository.findByUsername(username)
                .map(user -> user.getStatus() == Status.ONLINE)
                .orElse(false);
        log.trace("User {} online status: {}", username, online);
        return online;
    }

    /**
     * Check if username already exists.
     *
     * @param username the username to check
     * @return true if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Save a user entity.
     *
     * @param user the user to save
     * @return the saved user
     */
    public User save(User user) {
        log.debug("Saving user: {}", user.getUsername());
        User savedUser = userRepository.save(user);
        log.info("User saved: {}", savedUser.getUsername());
        return savedUser;
    }
}
