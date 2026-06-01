package com.chatsever.server.service;

import com.chatsever.server.model.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface ServerService {
    Server createServer(Server server, String ownerId);
    List<Server> getMyServers(String userId);
    Page<Server> getMyServers(String userId, Pageable pageable); // NF13
    Map<String, Object> getServerDetails(Long serverId);
    Server updateServer(Long serverId, Server serverDetails, String userId);
    void deleteServer(Long serverId, String userId);
    void joinServer(Long serverId, String inviteCode, String userId);
    void joinServerByCode(String inviteCode, String userId);
    void leaveServer(Long serverId, String userId);
    String generateNewInviteCode(Long serverId, String userId);
    void updateMemberRoles(Long serverId, String userId, List<Long> roleIds);
    void ensureMember(Long serverId, String userId);
}