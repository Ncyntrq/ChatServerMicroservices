package com.chatsever.server.repository;

import com.chatsever.server.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    boolean existsByInviteCode(String inviteCode);
}