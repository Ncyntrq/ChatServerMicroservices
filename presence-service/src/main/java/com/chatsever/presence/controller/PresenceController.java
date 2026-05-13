package com.chatsever.presence.controller;

import com.chatsever.presence.model.UserStatus;
import com.chatsever.presence.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/test")
    public String test() {
        return "Presence Service is ONLINE - Chào mừng bạn đến với phòng 8083!";
    }

    // Đã đổi sang @RequestHeader để nhận diện user từ Gateway
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(@RequestHeader("X-User-Id") String userId) {
        presenceService.connect(userId);
        return ResponseEntity.ok(Map.of("message", "User connected", "status", "ONLINE"));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(@RequestHeader("X-User-Id") String userId) {
        presenceService.disconnect(userId);
        return ResponseEntity.ok(Map.of("message", "User disconnected", "status", "OFFLINE"));
    }

    @GetMapping("/online")
    public ResponseEntity<List<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<UserStatus> getStatus(@PathVariable String userId) {
        return ResponseEntity.ok(presenceService.getUserStatus(userId));
    }

    // THÊM MỚI (P5): API cho phép user tự đổi trạng thái (VD: Đang bận)
    @PutMapping("/status")
    public ResponseEntity<Map<String, String>> updateCustomStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UserStatus.Status status) {
        presenceService.updateCustomStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }
}