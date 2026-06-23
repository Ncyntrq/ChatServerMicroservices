package com.chatsever.messaging.repository;

import com.chatsever.messaging.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
