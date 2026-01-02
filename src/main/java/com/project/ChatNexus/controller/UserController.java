package com.project.ChatNexus.controller;

import com.project.ChatNexus.dto.response.UserResponse;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller handling user-related operations including WebSocket
 * events for user presence and REST endpoints for user queries.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management and presence operations")
public class UserController {

    private final UserService userService;

    /**
     * Handle user connection via WebSocket.
     * Saves/updates user status and broadcasts to all connected clients.
     *
     * @param user the connecting user
     * @return the user object for broadcast
     */
    @MessageMapping("/user.addUser")
    @SendTo("/topic/public")
    public User addUser(@Payload User user) {
        log.info("User connecting: {} ({})", user.getUsername(), user.getFullName());
        userService.saveUser(user);
        log.debug("User {} status set to ONLINE", user.getUsername());
        return user;
    }

    /**
     * Handle user disconnection via WebSocket.
     * Updates user status and broadcasts to all connected clients.
     *
     * @param user the disconnecting user
     * @return the user object for broadcast
     */
    @MessageMapping("/user.disconnectUser")
    @SendTo("/topic/public")
    public User disconnect(@Payload User user) {
        log.info("User disconnecting: {}", user.getUsername());
        userService.disconnect(user);
        log.debug("User {} status set to OFFLINE", user.getUsername());
        return user;
    }

    @Operation(
            summary = "Get connected users",
            description = "Retrieve all currently online users"
    )
    @ApiResponse(responseCode = "200", description = "List of online users")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> findConnectedUsers() {
        log.debug("Fetching connected users");
        List<UserResponse> users = userService.findConnectedUsers().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
        log.debug("Found {} connected users", users.size());
        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieve all registered users"
    )
    @ApiResponse(responseCode = "200", description = "List of all users")
    @GetMapping("/users/all")
    public ResponseEntity<List<UserResponse>> findAllUsers() {
        log.debug("Fetching all users");
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
        log.debug("Found {} total users", users.size());
        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Search users",
            description = "Search users by username (partial match)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results",
                    content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Search query for username") @RequestParam String query
    ) {
        log.info("Searching users with query: {}", query);
        List<UserResponse> users = userService.searchUsers(query).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
        log.debug("Search returned {} results", users.size());
        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Get user by username",
            description = "Retrieve a specific user by their username"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{username}")
    public ResponseEntity<?> getUserByUsername(
            @Parameter(description = "Username to look up") @PathVariable String username
    ) {
        log.debug("Looking up user: {}", username);
        return userService.findByUsername(username)
                .map(user -> {
                    log.debug("User found: {}", username);
                    return ResponseEntity.ok(mapToUserResponse(user));
                })
                .orElseGet(() -> {
                    log.debug("User not found: {}", username);
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(
            summary = "Check if user is online",
            description = "Check the online status of a specific user"
    )
    @ApiResponse(responseCode = "200", description = "Online status returned")
    @GetMapping("/users/{username}/online")
    public ResponseEntity<Map<String, Boolean>> isUserOnline(
            @Parameter(description = "Username to check") @PathVariable String username
    ) {
        boolean online = userService.isUserOnline(username);
        log.debug("User {} online status: {}", username, online);
        return ResponseEntity.ok(Map.of("online", online));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .build();
    }
}
