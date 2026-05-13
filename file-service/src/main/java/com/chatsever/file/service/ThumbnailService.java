package com.chatsever.file.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Service tạo thumbnail cho file ảnh.
 * Resize ảnh xuống 200x200 và upload lên MinIO bucket "chat-thumbnails".
 */
@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);
    private static final int THUMB_WIDTH = 200;
    private static final int THUMB_HEIGHT = 200;

    private final MinioClient minioClient;

    @Value("${minio.buckets.thumbnails:chat-thumbnails}")
    private String thumbnailBucket;

    public ThumbnailService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Generate thumbnail và upload lên MinIO.
     * @param originalStream InputStream của file ảnh gốc
     * @param storedName tên file gốc trên MinIO
     * @return key thumbnail trên MinIO
     */
    public String generateAndUpload(InputStream originalStream, String storedName) {
        try {
            // 1. Dùng Thumbnailator resize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(originalStream)
                    .size(THUMB_WIDTH, THUMB_HEIGHT)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

            // 2. Tạo thumbnail key
            String thumbKey = "thumb_" + removeExtension(storedName) + ".jpg";

            // 3. Upload thumbnail lên MinIO bucket "chat-thumbnails"
            byte[] thumbBytes = baos.toByteArray();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(thumbnailBucket)
                    .object(thumbKey)
                    .stream(new ByteArrayInputStream(thumbBytes), thumbBytes.length, -1)
                    .contentType("image/jpeg")
                    .build());

            log.info("Thumbnail uploaded: bucket={}, key={}", thumbnailBucket, thumbKey);
            return thumbKey;

        } catch (Exception e) {
            log.error("Lỗi tạo thumbnail cho file {}: {}", storedName, e.getMessage());
            return null;    // Không block upload chính nếu thumbnail fail
        }
    }

    /** Loại bỏ extension khỏi filename */
    private String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
}
