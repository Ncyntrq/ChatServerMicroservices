package com.chatsever.auth.controller;

import com.chatsever.common.dto.AuthRequest;
import com.chatsever.common.dto.AuthResponse;
import com.chatsever.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validate(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.ok(authService.validateToken(token));
    }
}