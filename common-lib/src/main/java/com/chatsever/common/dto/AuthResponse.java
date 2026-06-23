package com.chatsever.common.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;

    // Backward-compatible constructor (token + username only)
    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}