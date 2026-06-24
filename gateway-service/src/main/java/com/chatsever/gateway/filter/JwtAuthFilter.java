package com.chatsever.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Bơm định danh đã xác thực vào header cho downstream (P2.3/P2.4).
 *
 * <p>Việc verify JWT do Spring Security ({@code SecurityConfig} + dual-mode decoder)
 * đảm nhiệm. Filter này chỉ đọc {@link Jwt} từ {@code SecurityContext} rồi set:
 * <ul>
 *   <li>{@code X-User-Id} / {@code X-Username} = {@code preferred_username} (Keycloak)
 *       hoặc {@code sub} (token HMAC cũ = username) — giữ định danh theo username để
 *       không phá dữ liệu downstream đang gắn theo username.</li>
 *   <li>{@code X-User-Roles} = realm_access.roles (Keycloak), rỗng với token cũ.</li>
 * </ul>
 *
 * <p>Path public ({@code /api/auth/**}, {@code /ws/**}, {@code /actuator/**}) không có
 * authentication → đi qua nguyên trạng.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(auth -> withIdentityHeaders(exchange, ((JwtAuthenticationToken) auth).getToken()))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getSubject();
        }
        String roles = String.join(",", extractRealmRoles(jwt));

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", username)
                .header("X-Username", username)
                .header("X-User-Roles", roles)
                .build();
        return exchange.mutate().request(mutated).build();
    }

    /** Lấy {@code realm_access.roles} từ claim Keycloak; rỗng nếu không có. */
    @SuppressWarnings("unchecked")
    private Collection<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            return (Collection<String>) roles;
        }
        return List.of();
    }

    @Override
    public int getOrder() {
        return -1; // Bơm header trước khi route tới downstream.
    }
}
