package com.chatsever.messaging.infrastructure.persistence;

import com.chatsever.messaging.domain.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop50ByStatusOrderByCreatedAtAsc(String status);
    List<OutboxMessage> findByAggregateTypeAndAggregateIdIn(String type, List<String> ids);
}
