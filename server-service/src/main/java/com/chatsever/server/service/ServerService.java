package com.chatsever.server.service;

import com.chatsever.server.model.Server;
import java.util.List;

public interface ServerService {
    Server createServer(Server server, String ownerId);
    List<Server> getAllServers();
}