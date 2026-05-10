package com.chatsever.server.controller;

import com.chatsever.server.model.Server;
import com.chatsever.server.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    @Autowired
    private ServerRepository serverRepository;

    // API 1: Tạo Server (Nhóm) mới
    @PostMapping
    public ResponseEntity<Server> createServer(
            @RequestBody Server server,
            @RequestHeader("X-User-Id") String ownerId) { // Bắt lấy ID từ Gateway

        server.setOwnerId(ownerId); // Gắn người này làm chủ Nhóm
        Server savedServer = serverRepository.save(server);
        return ResponseEntity.ok(savedServer);
    }

    // API 2: Lấy danh sách các Nhóm đang có
    @GetMapping
    public ResponseEntity<List<Server>> getAllServers() {
        return ResponseEntity.ok(serverRepository.findAll());
    }
}