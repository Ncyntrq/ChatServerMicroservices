package com.chatsever.server.controller;

import com.chatsever.server.model.Server;
import com.chatsever.server.service.ServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private final ServerService serverService;

    // Dùng Constructor Injection cho chuyên nghiệp
    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @PostMapping
    public ResponseEntity<Server> createServer(
            @RequestBody Server server,
            @RequestHeader("X-User-Id") String ownerId) {
        // Bây giờ Controller chỉ gọi Service xử lý thôi
        return ResponseEntity.ok(serverService.createServer(server, ownerId));
    }

    @GetMapping
    public ResponseEntity<List<Server>> getAllServers() {
        return ResponseEntity.ok(serverService.getAllServers());
    }
}