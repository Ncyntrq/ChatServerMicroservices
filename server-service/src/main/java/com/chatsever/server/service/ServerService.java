package com.chatsever.server.service;

import com.chatsever.server.model.Server;
import java.util.List;
import java.util.Map;

public interface ServerService {
    Server createServer(Server server, String ownerId);
    List<Server> getMyServers(String userId);
    Map<String, Object> getServerDetails(Long serverId);
    Server updateServer(Long serverId, Server serverDetails, String userId);
    void deleteServer(Long serverId, String userId);
    void joinServer(Long serverId, String inviteCode, String userId);
    void leaveServer(Long serverId, String userId);
    String generateNewInviteCode(Long serverId, String userId);
}