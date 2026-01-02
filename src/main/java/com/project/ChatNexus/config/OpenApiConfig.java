package com.project.ChatNexus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Access the documentation at /swagger-ui.html or /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("ChatNexus API")
                        .version("3.0.0")
                        .description("""
                                ChatNexus is a real-time 1-to-1 chat application built with Spring Boot and WebSocket.
                                
                                ## Features
                                - Real-time messaging via WebSocket/STOMP
                                - Media sharing (images, videos, audio) via Cloudinary
                                - JWT authentication
                                - Read receipts and delivery status
                                - User presence (online/offline)
                                
                                ## WebSocket Endpoints
                                - Connect: `/ws` (with SockJS fallback)
                                - Send message: `/app/chat`
                                - Mark as read: `/app/chat.read`
                                - Subscribe to messages: `/user/{username}/queue/messages`
                                - Subscribe to status: `/user/{username}/queue/status`
                                """)
                        .contact(new Contact()
                                .name("ChatNexus Support")
                                .email("support@chatnexus.com")
                                .url("https://github.com/yourusername/ChatNexus"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.chatnexus.com")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/auth/login")));
    }
}

