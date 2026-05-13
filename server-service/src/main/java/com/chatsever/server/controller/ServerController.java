package com.chatsever.server.controller;

import com.chatsever.server.model.Server;
import com.chatsever.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {
    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<Server> create(@RequestBody Server s, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.createServer(s, uid));
    }

    @GetMapping
    public ResponseEntity<List<Server>> getMy(@RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.getMyServers(uid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerDetails(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Server> update(@PathVariable Long id, @RequestBody Server s, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.updateServer(id, s, uid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        serverService.deleteServer(id, uid);
        return ResponseEntity.ok(Map.of("message", "Đã xóa server thành công"));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, String>> join(@PathVariable Long id, @RequestParam String code, @RequestHeader("X-User-Id") String uid) {
        serverService.joinServer(id, code, uid);
        return ResponseEntity.ok(Map.of("message", "Gia nhập thành công"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Map<String, String>> leave(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        serverService.leaveServer(id, uid);
        return ResponseEntity.ok(Map.of("message", "Đã rời khỏi server"));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<Map<String, String>> createInvite(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(Map.of("inviteCode", serverService.generateNewInviteCode(id, uid)));
    }
}