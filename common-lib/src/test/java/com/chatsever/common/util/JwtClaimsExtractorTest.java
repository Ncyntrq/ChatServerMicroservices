package com.chatsever.common.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtClaimsExtractorTest {

    private static final String SECRET = "chatsever-jwt-secret-key-2026-safe-key-local-dev";

    private String tokenFor(String username, String secret, long expMillisFromNow) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMillisFromNow))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void verifyAndGetSubject_validToken_returnsUsername() {
        String token = tokenFor("alice", SECRET, 60_000);
        assertEquals("alice", JwtClaimsExtractor.verifyAndGetSubject(token, SECRET));
    }

    @Test
    void verifyAndGetSubject_wrongSecret_throws() {
        String token = tokenFor("bob", SECRET, 60_000);
        assertThrows(JwtException.class,
                () -> JwtClaimsExtractor.verifyAndGetSubject(token, SECRET + "-tampered"));
    }

    @Test
    void verifyAndGetSubject_expiredToken_throws() {
        String token = tokenFor("carol", SECRET, -1_000); // đã hết hạn
        assertThrows(JwtException.class,
                () -> JwtClaimsExtractor.verifyAndGetSubject(token, SECRET));
    }

    @Test
    void verifyAndGetSubject_malformedToken_throws() {
        assertThrows(JwtException.class,
                () -> JwtClaimsExtractor.verifyAndGetSubject("not-a-jwt", SECRET));
    }
}
