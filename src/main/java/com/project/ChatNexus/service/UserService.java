package com.project.ChatNexus.service;

import com.project.ChatNexus.model.Status;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void saveUser(User user) {
        var storedUser = userRepository.findByUsername(user.getUsername())
                .orElse(null);

        if (storedUser != null) {
            storedUser.setStatus(Status.ONLINE);
            storedUser.setLastSeen(LocalDateTime.now());
            userRepository.save(storedUser);
        }
    }

    public void disconnect(User user) {
        var storedUser = userRepository.findByUsername(user.getUsername())
                .orElse(null);

        if (storedUser != null) {
            storedUser.setStatus(Status.OFFLINE);
            storedUser.setLastSeen(LocalDateTime.now());
            userRepository.save(storedUser);
        }
    }

    public List<User> findConnectedUsers() {
        return userRepository.findAllByStatus(Status.ONLINE);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }

    public boolean isUserOnline(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getStatus() == Status.ONLINE)
                .orElse(false);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
