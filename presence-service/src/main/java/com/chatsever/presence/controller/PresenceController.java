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

    /**
     * P1 — Đăng ký online.
     * Hỗ trợ 2 cách gọi:
     * - Từ Gateway (qua header): X-User-Id
     * - Từ messaging-service internal (qua query param): ?username=xxx
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "username", required = false) String paramUsername) {
        String userId = resolveUserId(headerUserId, paramUsername);
        presenceService.connect(userId);
        return ResponseEntity.ok(Map.of("message", "User connected", "status", "ONLINE"));
    }

    /**
     * P2 — Đăng ký offline.
     * Hỗ trợ cả header và query param.
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "username", required = false) String paramUsername) {
        String userId = resolveUserId(headerUserId, paramUsername);
        presenceService.disconnect(userId);
        return ResponseEntity.ok(Map.of("message", "User disconnected", "status", "OFFLINE"));
    }

    /** P3 — Danh sách online (giữ cho tương thích ngược) */
    @GetMapping("/online")
    public ResponseEntity<List<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    /** P3.1 — Lấy toàn bộ trạng thái user (từ điển userId -> status) */
    @GetMapping("/all-statuses")
    public ResponseEntity<Map<String, String>> getAllStatuses() {
        return ResponseEntity.ok(presenceService.getAllStatuses());
    }

    /** P4 — Kiểm tra trạng thái user */
    @GetMapping("/status/{userId}")
    public ResponseEntity<UserStatus> getStatus(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(presenceService.getUserStatus(userId));
    }

    /** P5 — Đổi trạng thái tùy chỉnh (ONLINE, IDLE, DO_NOT_DISTURB, INVISIBLE) */
    @PutMapping("/status")
    public ResponseEntity<Map<String, String>> updateCustomStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("status") UserStatus.Status status) {
        presenceService.updateCustomStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }

    /**
     * Helper: ưu tiên header X-User-Id, fallback sang query param username.
     */
    private String resolveUserId(String headerUserId, String paramUsername) {
        if (headerUserId != null && !headerUserId.isBlank())
            return headerUserId;
        if (paramUsername != null && !paramUsername.isBlank())
            return paramUsername;
        throw new IllegalArgumentException("Thiếu thông tin userId (header X-User-Id hoặc param username)");
    }
}