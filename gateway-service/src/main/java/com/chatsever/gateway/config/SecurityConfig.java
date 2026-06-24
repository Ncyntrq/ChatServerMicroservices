package com.chatsever.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Cấu hình bảo mật cho API Gateway (WebFlux) — P2.3.
 *
 * <p>Gateway đóng vai trò OAuth2 Resource Server: mọi request (trừ các path public)
 * phải mang JWT hợp lệ. Việc verify dùng {@link DualModeReactiveJwtDecoder}:
 * Keycloak (RS256) trước, fallback HMAC token cũ.
 *
 * <p>Header định danh ({@code X-User-Id}, {@code X-Username}, {@code X-User-Roles})
 * cho downstream được bơm bởi {@code JwtAuthFilter} sau khi xác thực.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(ex -> ex
                        // Đăng nhập/đăng ký, WebSocket (tự verify token ở messaging),
                        // và actuator → không yêu cầu token tại gateway.
                        .pathMatchers("/api/auth/**", "/ws/**", "/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                .build();
    }

    /**
     * Decoder dual-mode. Tự định nghĩa bean nên Spring Boot không tạo decoder mặc định.
     * Keycloak dùng jwk-set-uri (nạp lazy) → không chặn startup khi Keycloak chưa sẵn sàng.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${jwt.secret}") String hmacSecret) {

        ReactiveJwtDecoder keycloak = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();

        SecretKey key = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        ReactiveJwtDecoder hmac = NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return new DualModeReactiveJwtDecoder(keycloak, hmac);
    }
}
