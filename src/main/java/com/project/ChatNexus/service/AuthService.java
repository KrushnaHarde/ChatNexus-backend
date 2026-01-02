package com.project.ChatNexus.service;

import com.project.ChatNexus.dto.request.LoginRequest;
import com.project.ChatNexus.dto.request.RegisterRequest;
import com.project.ChatNexus.dto.response.AuthResponse;
import com.project.ChatNexus.model.Status;
import com.project.ChatNexus.model.User;
import com.project.ChatNexus.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service handling user authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return authentication response with JWT token
     * @throws RuntimeException if username already exists
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration for username: {}", request.getUsername());

        if (userService.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(Status.OFFLINE)
                .lastSeen(LocalDateTime.now())
                .build();

        userService.save(user);
        log.debug("User saved to database: {}", user.getUsername());

        String token = jwtService.generateToken(user);
        log.info("Registration successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .message("Registration successful")
                .build();
    }

    /**
     * Authenticate a user and generate JWT token.
     *
     * @param request login credentials
     * @return authentication response with JWT token
     * @throws RuntimeException if credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for username: {}", request.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found after successful authentication: {}", request.getUsername());
                    return new RuntimeException("User not found");
                });

        String token = jwtService.generateToken(user);
        log.info("Login successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .message("Login successful")
                .build();
    }
}

