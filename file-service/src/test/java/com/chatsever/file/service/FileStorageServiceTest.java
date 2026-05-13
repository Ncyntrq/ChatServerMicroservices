package com.chatsever.file.service;


import com.chatsever.file.model.FileMetadata;
import com.chatsever.file.repository.FileMetadataRepository;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho FileStorageService.
 * MinIO và DB đều được mock.
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private FileMetadataRepository repository;

    @Mock
    private ThumbnailService thumbnailService;

    @InjectMocks
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Mockito doesn't process
        ReflectionTestUtils.setField(fileStorageService, "filesBucket", "chat-files");
        ReflectionTestUtils.setField(fileStorageService, "thumbnailsBucket", "chat-thumbnails");
    }

    @Test
    void upload_validImageFile_shouldSucceed() throws Exception {
        // Arrange
        byte[] content = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", content
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(thumbnailService.generateAndUpload(any(), anyString())).thenReturn("thumb_test.jpg");
        when(repository.save(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        // Act
        var result = fileStorageService.upload(file, "user1", 1L);

        // Assert
        assertNotNull(result);
        assertEquals("test.png", result.getOriginalName());
        assertEquals("image/png", result.getContentType());
        assertEquals(5L, result.getFileSize());
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(thumbnailService).generateAndUpload(any(), anyString());
        verify(repository).save(any(FileMetadata.class));
    }

    @Test
    void upload_textFile_shouldNotGenerateThumbnail() throws Exception {
        byte[] content = "hello world".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "readme.txt", "text/plain", content
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(repository.save(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = fileStorageService.upload(file, "user1", 1L);

        assertNotNull(result);
        assertNull(result.getThumbnailUrl());
        verify(thumbnailService, never()).generateAndUpload(any(), anyString());
    }

    @Test
    void upload_emptyFile_shouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.upload(file, "user1", 1L));
    }

    @Test
    void upload_unsupportedType_shouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hack.exe", "application/x-msdownload", new byte[]{1, 2, 3}
        );

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.upload(file, "user1", 1L));
    }

    @Test
    void deleteFile_notOwner_shouldThrowException() {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        metadata.setUploader("owner");

        when(repository.findById(1L)).thenReturn(java.util.Optional.of(metadata));

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.deleteFile(1L, "notOwner"));
    }
}
