package com.chatsever.file.repository;

import com.chatsever.file.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho bảng file_metadata.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /** Tìm file theo uploader */
    List<FileMetadata> findByUploader(String uploader);

    /** Tìm file theo channelId */
    List<FileMetadata> findByChannelId(Long channelId);

    /** Tìm file theo channelId, mới nhất trước (dùng cho sidebar Ảnh/Video & File). */
    List<FileMetadata> findByChannelIdOrderByCreatedAtDesc(Long channelId);
}
