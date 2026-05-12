package com.chatsever.messaging.repository;

import com.chatsever.messaging.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelIdOrderByIdDesc(Long channelId, Pageable pageable);
    List<ChatMessage> findByChannelIdAndIdLessThanOrderByIdDesc(Long channelId, Long id, Pageable pageable);
}
