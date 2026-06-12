package com.chatsever.file.controller;

import com.chatsever.file.dto.FileMetadataDTO;

import com.chatsever.file.service.FileStorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * REST API cho file-service.
 * 5 endpoints chính + actuator health check.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 1. Upload file (max 10MB).
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     */
    @PostMapping("/upload")
    public ResponseEntity<FileMetadataDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "channelId", required = false) Long channelId) {
        FileMetadataDTO result = fileStorageService.upload(file, userId, channelId);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. Download file gốc.
     * GET /api/files/{fileId}
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) {
        FileMetadataDTO info = fileStorageService.getFileInfo(fileId);
        InputStream stream = fileStorageService.download(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(info.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.getOriginalName() + "\"")
                .body(new InputStreamResource(stream));
    }

    /**
     * 3. Download thumbnail.
     * GET /api/files/{fileId}/thumbnail
     */
    @GetMapping("/{fileId}/thumbnail")
    public ResponseEntity<InputStreamResource> downloadThumbnail(@PathVariable Long fileId) {
        InputStream stream = fileStorageService.downloadThumbnail(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(stream));
    }

    /**
     * 4. Thông tin file (size, type, name).
     * GET /api/files/{fileId}/info
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<FileMetadataDTO> getFileInfo(@PathVariable Long fileId) {
        FileMetadataDTO info = fileStorageService.getFileInfo(fileId);
        return ResponseEntity.ok(info);
    }

    /**
     * 6. Liệt kê file theo channel (cho sidebar Ảnh/Video & File).
     * GET /api/files/channel/{channelId}?type=image|document&limit=20
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<java.util.List<FileMetadataDTO>> getFilesByChannel(
            @PathVariable Long channelId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(fileStorageService.getFilesByChannel(channelId, type, limit));
    }

    /**
     * 5. Xóa file (chỉ owner).
     * DELETE /api/files/{fileId}?userId=xxx
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @RequestParam String userId) {
        fileStorageService.deleteFile(fileId, userId);
        return ResponseEntity.ok().build();
    }
}
