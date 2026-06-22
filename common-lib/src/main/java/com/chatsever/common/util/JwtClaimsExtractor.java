package com.chatsever.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tiện ích parse JWT Claims phi tập trung (P2.4).
 *
 * <p>Hai cách dùng:
 * <ul>
 *   <li>{@link #verifyAndGetSubject(String, String)} — service tự verify chữ ký JWT bằng
 *       shared secret (không gọi lại auth-service). Dùng cho WebSocket / nơi nhận token trực tiếp.</li>
 *   <li>{@code getUserId/getUsername/getRoles(HttpServletRequest)} — đọc identity mà
 *       gateway đã verify và bơm vào header (luồng REST đi qua gateway).</li>
 * </ul>
 *
 * Khớp định dạng token của auth-service: HMAC-SHA, subject = username.
 */
public final class JwtClaimsExtractor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLES = "X-User-Roles";

    private JwtClaimsExtractor() {
    }

    /**
     * Verify chữ ký JWT cục bộ và trả về subject (username).
     *
     * @throws io.jsonwebtoken.JwtException nếu token sai chữ ký, hết hạn hoặc không hợp lệ.
     */
    public static String verifyAndGetSubject(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /** Lấy userId (gateway hiện set = username) từ header. Null nếu thiếu. */
    public static String getUserId(HttpServletRequest request) {
        return request.getHeader(HEADER_USER_ID);
    }

    /** Lấy username từ header. Null nếu thiếu. */
    public static String getUsername(HttpServletRequest request) {
        return request.getHeader(HEADER_USERNAME);
    }

    /** Lấy tập role từ header {@code X-User-Roles} (phân tách bằng dấu phẩy). Rỗng nếu thiếu. */
    public static Set<String> getRoles(HttpServletRequest request) {
        String roles = request.getHeader(HEADER_ROLES);
        if (roles == null || roles.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /** Kiểm tra request có role chỉ định không. */
    public static boolean hasRole(HttpServletRequest request, String role) {
        return getRoles(request).contains(role);
    }
}
