package com.chatsever.gateway.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

/**
 * Decoder chế độ chuyển tiếp (dual-mode) cho giai đoạn migrate sang Keycloak.
 *
 * <p>Thử verify bằng Keycloak (RS256, qua JWKS) trước; nếu thất bại thì fallback
 * verify token HMAC cũ do auth-service phát hành (HS256, shared secret).
 *
 * <p>Khi toàn bộ client đã chuyển sang Keycloak → bỏ {@code hmac} và dùng thẳng
 * {@code NimbusReactiveJwtDecoder} của Keycloak.
 */
public class DualModeReactiveJwtDecoder implements ReactiveJwtDecoder {

    private final ReactiveJwtDecoder keycloak;
    private final ReactiveJwtDecoder hmac;

    public DualModeReactiveJwtDecoder(ReactiveJwtDecoder keycloak, ReactiveJwtDecoder hmac) {
        this.keycloak = keycloak;
        this.hmac = hmac;
    }

    @Override
    public Mono<Jwt> decode(String token) {
        // Keycloak RS256 trước, lỗi (sai alg/kid/chữ ký) thì thử HMAC token cũ.
        return keycloak.decode(token)
                .onErrorResume(e -> hmac.decode(token));
    }
}
