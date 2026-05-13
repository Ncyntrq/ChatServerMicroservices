package com.chatsever.file.service;

import io.minio.MinioClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit test cho ThumbnailService.
 * Kiểm tra generate thumbnail + upload lên MinIO.
 */
@ExtendWith(MockitoExtension.class)
class ThumbnailServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private ThumbnailService thumbnailService;

    @Test
    void generateAndUpload_nullInputStream_shouldReturnNull() {
        // Khi inputstream gây lỗi → trả null (không block upload chính)
        String result = thumbnailService.generateAndUpload(null, "test.png");
        assertNull(result);
    }

    @Test
    void generateAndUpload_invalidImage_shouldReturnNull() {
        // Khi input không phải ảnh hợp lệ → trả null
        ByteArrayInputStream fakeInput = new ByteArrayInputStream(new byte[]{1, 2, 3});
        String result = thumbnailService.generateAndUpload(fakeInput, "bad.png");
        assertNull(result);
    }
}
