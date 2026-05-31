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
}
