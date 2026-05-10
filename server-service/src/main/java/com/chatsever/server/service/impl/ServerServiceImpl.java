package com.chatsever.server.service.impl;

import com.chatsever.server.model.Server;
import com.chatsever.server.repository.ServerRepository;
import com.chatsever.server.service.ServerService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service // Bắt buộc phải có để Spring nhận diện đây là lớp Service
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;

    // Viết Constructor tay thay vì dùng Lombok để tránh lỗi đỏ nãy giờ
    public ServerServiceImpl(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Override
    public Server createServer(Server server, String ownerId) {
        server.setOwnerId(ownerId); // Chuyển logic từ Controller sang đây
        return serverRepository.save(server);
    }

    @Override
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }
}