package com.chatsever.messaging.repository;

import com.chatsever.messaging.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelIdOrderByIdDesc(Long channelId, Pageable pageable);
    List<ChatMessage> findByChannelIdAndIdLessThanOrderByIdDesc(Long channelId, Long id, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1) ORDER BY m.id DESC")
    List<ChatMessage> findPrivateMessages(@Param("u1") String u1, @Param("u2") String u2, Pageable pageable);
    
    @Query("SELECT m FROM ChatMessage m WHERE ((m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)) AND m.id < :beforeId ORDER BY m.id DESC")
    List<ChatMessage> findPrivateMessagesBefore(@Param("u1") String u1, @Param("u2") String u2, @Param("beforeId") Long beforeId, Pageable pageable);

    // --- Tìm kiếm tin nhắn theo từ khóa (chứa-từ-khóa, không phân biệt hoa thường) ---

    // Tìm trong 1 channel
    @Query("SELECT m FROM ChatMessage m WHERE m.channelId = :channelId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
    List<ChatMessage> searchInChannel(@Param("channelId") Long channelId, @Param("q") String q, Pageable pageable);

    // Tìm trong cuộc trò chuyện DM giữa 2 user
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "((m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)) " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
    List<ChatMessage> searchInPrivate(@Param("u1") String u1, @Param("u2") String u2, @Param("q") String q, Pageable pageable);

    // Tìm toàn cục theo user (mọi tin nhắn user gửi hoặc nhận)
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user OR m.receiver = :user) " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.id DESC")
    List<ChatMessage> searchAllForUser(@Param("user") String user, @Param("q") String q, Pageable pageable);

    // serverId của 1 channel (suy ra từ chính tin nhắn của channel) — dùng để kiểm tra membership
    @Query("SELECT m.serverId FROM ChatMessage m WHERE m.channelId = :channelId AND m.serverId IS NOT NULL ORDER BY m.id DESC")
    List<Long> findServerIdsByChannel(@Param("channelId") Long channelId, Pageable pageable);

    // --- API search dùng bởi MessageController (/search) ---
    @Query("SELECT m FROM ChatMessage m WHERE m.channelId = :channelId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.id DESC")
    List<ChatMessage> searchByChannel(@Param("channelId") Long channelId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.serverId = :serverId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.id DESC")
    List<ChatMessage> searchByServer(@Param("serverId") Long serverId, @Param("keyword") String keyword, Pageable pageable);
}
