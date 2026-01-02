package com.project.ChatNexus.controller;

import com.project.ChatNexus.dto.response.MediaUploadResponse;
import com.project.ChatNexus.model.MessageType;
import com.project.ChatNexus.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Controller handling media upload operations via Cloudinary.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Media upload and management operations")
public class MediaController {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String ERROR_KEY = "error";

    private final CloudinaryService cloudinaryService;

    @Operation(
            summary = "Upload media file",
            description = """
                    Upload media file to Cloudinary for sharing in chat.
                    
                    **Supported formats:**
                    - Images: JPG, JPEG, PNG, GIF, WebP, BMP
                    - Videos: MP4, MOV, AVI, MKV, WebM
                    - Audio: MP3, WAV, OGG, M4A, AAC
                    
                    **Max file size:** 50MB
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or file type not supported"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Media upload request received - filename: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // Validate file is not empty
            if (file.isEmpty()) {
                log.warn("Upload rejected: empty file");
                return ResponseEntity.badRequest()
                        .body(Map.of(ERROR_KEY, "Please select a file to upload"));
            }

            // Check file size (50MB max)
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("Upload rejected: file too large ({} bytes)", file.getSize());
                return ResponseEntity.badRequest()
                        .body(Map.of(ERROR_KEY, "File size exceeds maximum limit of 50MB"));
            }

            // Check if file type is allowed
            if (!cloudinaryService.isFileAllowed(file.getOriginalFilename())) {
                log.warn("Upload rejected: unsupported file type - {}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(Map.of(ERROR_KEY, "File type not supported. Allowed: " + cloudinaryService.getAllowedExtensions()));
            }

            // Upload to Cloudinary
            log.debug("Uploading file to Cloudinary...");
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file);

            MediaUploadResponse response = MediaUploadResponse.builder()
                    .url((String) uploadResult.get("url"))
                    .publicId((String) uploadResult.get("publicId"))
                    .messageType((MessageType) uploadResult.get("messageType"))
                    .fileName((String) uploadResult.get("fileName"))
                    .fileSize((Long) uploadResult.get("fileSize"))
                    .mimeType((String) uploadResult.get("mimeType"))
                    .build();

            log.info("File uploaded successfully - url: {}, type: {}", response.getUrl(), response.getMessageType());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Upload rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @Operation(
            summary = "Get allowed file extensions",
            description = "Retrieve list of supported file extensions for media upload"
    )
    @ApiResponse(responseCode = "200", description = "List of allowed extensions")
    @GetMapping("/allowed-extensions")
    public ResponseEntity<Map<String, String>> getAllowedExtensions() {
        log.debug("Fetching allowed file extensions");
        return ResponseEntity.ok(Map.of("extensions", cloudinaryService.getAllowedExtensions()));
    }
}
