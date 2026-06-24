package com.chatsever.server.repository;

import com.chatsever.server.model.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    Optional<Server> findByInviteCode(String inviteCode);
    Page<Server> findByIdIn(List<Long> ids, Pageable pageable); // NF13
}
