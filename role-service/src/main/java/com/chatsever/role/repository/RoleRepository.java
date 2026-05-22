package com.chatsever.role.repository;

import com.chatsever.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findByServerId(String serverId);
    List<Role> findByServerIdOrderByPriorityDesc(String serverId);
    Optional<Role> findByServerIdAndRoleName(String serverId, String roleName);
    List<Role> findByIdIn(List<String> ids);
}