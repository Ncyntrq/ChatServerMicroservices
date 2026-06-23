package com.chatsever.server.repository;

import com.chatsever.server.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByUserId(String userId);
    List<Member> findByServerId(Long serverId);
    Optional<Member> findByServerIdAndUserId(Long serverId, String userId);
    boolean existsByServerIdAndUserId(Long serverId, String userId);
    void deleteByServerId(Long serverId);
}
