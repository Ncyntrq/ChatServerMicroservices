package com.chatsever.notification.repository;

import com.chatsever.notification.model.UnreadCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnreadCounterRepository extends JpaRepository<UnreadCounter, Long> {
    List<UnreadCounter> findByUserId(String userId);
    
    Optional<UnreadCounter> findByUserIdAndChannelId(String userId, Long channelId);
    
    Optional<UnreadCounter> findByUserIdAndSenderUsername(String userId, String senderUsername);
}
