package com.chatsever.auth.controller;

import com.chatsever.auth.dto.ChangePasswordRequest;
import com.chatsever.common.dto.AuthRequest;
import com.chatsever.common.dto.AuthResponse;
import com.chatsever.auth.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // A1 — Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // A2 — Đăng nhập (trả access + refresh token)
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // A3 — Xác thực token
    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validate(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // A5 — Refresh token: cấp access token mới
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ResponseEntity.ok(authService.refreshAccessToken(refreshToken));
    }

    // A6 — Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("X-User-Id") String username,
            @RequestBody ChangePasswordRequest request) {
        String result = authService.changePassword(username, request);
        return ResponseEntity.ok(Map.of("message", result));
    }
}