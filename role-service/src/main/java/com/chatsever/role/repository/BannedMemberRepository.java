package com.chatsever.role.repository;

import com.chatsever.role.model.BannedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannedMemberRepository extends JpaRepository<BannedMember, String> {
    boolean existsByServerIdAndUserId(String serverId, String userId);
    Optional<BannedMember> findByServerIdAndUserId(String serverId, String userId);
    List<BannedMember> findByServerId(String serverId);
}
