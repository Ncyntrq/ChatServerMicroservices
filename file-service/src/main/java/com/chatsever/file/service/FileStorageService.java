package com.chatsever.file.service;

import com.chatsever.file.dto.FileMetadataDTO;
import com.chatsever.file.exception.FileNotFoundException;
import com.chatsever.file.exception.FileTooLargeException;
import com.chatsever.file.model.FileMetadata;
import com.chatsever.file.repository.FileMetadataRepository;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý upload/download file qua MinIO.
 * Hỗ trợ: ảnh, video nhỏ, PDF, document, code snippet.
 * Max file size: 10MB.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_TYPES = Set.of(
            // Ảnh
            "image/jpeg", "image/png", "image/gif", "image/webp",
            // Video nhỏ
            "video/mp4", "video/webm",
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Code / Text
            "text/plain", "application/json", "text/html", "text/css",
            "application/javascript", "text/x-java-source"
    );

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final MinioClient minioClient;
    private final FileMetadataRepository repository;
    private final ThumbnailService thumbnailService;

    @Value("${minio.buckets.files:chat-files}")
    private String filesBucket;

    @Value("${minio.buckets.thumbnails:chat-thumbnails}")
    private String thumbnailsBucket;

    public FileStorageService(MinioClient minioClient, FileMetadataRepository repository,
                              ThumbnailService thumbnailService) {
        this.minioClient = minioClient;
        this.repository = repository;
        this.thumbnailService = thumbnailService;
    }

    /**
     * Upload file lên MinIO + lưu metadata vào MySQL.
     */
    @Transactional
    public FileMetadataDTO upload(MultipartFile file, String userId, Long channelId) {
        // 1. Validate
        validateFile(file);

        // 2. Generate unique stored name: UUID + extension
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;

        try {
            // 3. Đảm bảo bucket tồn tại
            ensureBucketExists(filesBucket);

            // 4. Upload lên MinIO bucket "chat-files"
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(filesBucket)
                    .object(storedName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("File uploaded: bucket={}, key={}, size={}", filesBucket, storedName, file.getSize());

            // 5. Nếu là ảnh → generate thumbnail
            String thumbnailKey = null;
            if (isImage(file.getContentType())) {
                ensureBucketExists(thumbnailsBucket);
                thumbnailKey = thumbnailService.generateAndUpload(file.getInputStream(), storedName);
            }

            // 6. Lưu metadata vào MySQL
            FileMetadata metadata = new FileMetadata(
                    originalName, storedName, file.getContentType(),
                    file.getSize(), filesBucket, thumbnailKey,
                    userId, channelId
            );
            FileMetadata saved = repository.save(metadata);

            return toDTO(saved);

        } catch (Exception e) {
            log.error("Lỗi upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Download file gốc từ MinIO.
     */
    public InputStream download(Long fileId) {
        FileMetadata metadata = repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Không tìm thấy file với id: " + fileId));
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(metadata.getBucket())
                    .object(metadata.getStoredName())
                    .build());
        } catch (Exception e) {
            log.error("Lỗi download file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Không thể download file: " + e.getMessage(), e);
        }
    }

    /**
     * Download thumbnail từ MinIO.
     */
    public InputStream downloadThumbnail(Long fileId) {
        FileMetadata metadata = repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Không tìm thấy file với id: " + fileId));

        if (metadata.getThumbnailKey() == null) {
            throw new FileNotFoundException("File này không có thumbnail");
        }

        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(thumbnailsBucket)
                    .object(metadata.getThumbnailKey())
                    .build());
        } catch (Exception e) {
            log.error("Lỗi download thumbnail {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Không thể download thumbnail: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy thông tin metadata của file.
     */
    public FileMetadataDTO getFileInfo(Long fileId) {
        FileMetadata metadata = repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Không tìm thấy file với id: " + fileId));
        return toDTO(metadata);
    }

    /**
     * Xóa file — chỉ owner mới được xóa.
     */
    @Transactional
    public void deleteFile(Long fileId, String userId) {
        FileMetadata metadata = repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Không tìm thấy file với id: " + fileId));

        if (!metadata.getUploader().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xóa file này");
        }

        try {
            // Xóa file gốc trên MinIO
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(metadata.getBucket())
                    .object(metadata.getStoredName())
                    .build());

            // Xóa thumbnail nếu có
            if (metadata.getThumbnailKey() != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(thumbnailsBucket)
                        .object(metadata.getThumbnailKey())
                        .build());
            }

            // Xóa metadata khỏi DB
            repository.delete(metadata);

            log.info("Deleted file: id={}, name={}, by={}", fileId, metadata.getOriginalName(), userId);

        } catch (Exception e) {
            log.error("Lỗi xóa file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Không thể xóa file: " + e.getMessage(), e);
        }
    }

    /**
     * Liệt kê file của 1 channel, mới nhất trước.
     * @param type  "image" (chỉ ảnh) / "document" (loại trừ ảnh) / null (tất cả)
     * @param limit số lượng tối đa trả về (>0)
     */
    public List<FileMetadataDTO> getFilesByChannel(Long channelId, String type, int limit) {
        return repository.findByChannelIdOrderByCreatedAtDesc(channelId).stream()
                .filter(m -> matchesType(m.getContentType(), type))
                .limit(limit > 0 ? limit : 20)
                .map(this::toDTO)
                .toList();
    }

    /** Lọc theo nhóm loại: image = ảnh; document = không phải ảnh; null/khác = tất cả. */
    private boolean matchesType(String contentType, String type) {
        if (type == null || type.isBlank()) return true;
        boolean img = isImage(contentType);
        if ("image".equalsIgnoreCase(type)) return img;
        if ("document".equalsIgnoreCase(type)) return !img;
        return true;
    }

    // --- Private helpers ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException(
                    String.format("File %s vượt quá kích thước cho phép (%d MB)",
                            file.getOriginalFilename(), MAX_FILE_SIZE / (1024 * 1024)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Loại file không được hỗ trợ: " + contentType);
        }
    }

    private boolean isImage(String contentType) {
        return contentType != null && IMAGE_TYPES.contains(contentType);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot) : "";
    }

    private void ensureBucketExists(String bucket) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }

    /** Convert Entity → DTO */
    private FileMetadataDTO toDTO(FileMetadata m) {
        String url = "/api/files/" + m.getId();
        String thumbnailUrl = m.getThumbnailKey() != null ? "/api/files/" + m.getId() + "/thumbnail" : null;

        return new FileMetadataDTO(
                m.getId(), m.getOriginalName(), m.getContentType(), m.getFileSize(),
                url, thumbnailUrl, m.getUploader(), m.getChannelId(), m.getCreatedAt()
        );
    }
}
