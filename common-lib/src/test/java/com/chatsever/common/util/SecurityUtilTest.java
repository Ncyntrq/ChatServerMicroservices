package com.chatsever.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Test BCrypt hash/verify trong SecurityUtil */
class SecurityUtilTest {

    // Hash password rồi verify lại phải khớp
    @Test
    void hashedPasswordVerifiesAgainstOriginal() {
        String raw = "mypassword123";
        String hashed = SecurityUtil.hashPassword(raw);

        assertNotNull(hashed);
        assertNotEquals(raw, hashed, "Hash phải khác plaintext");
        assertTrue(hashed.startsWith("$2"), "BCrypt hash bắt đầu bằng $2");
        assertTrue(SecurityUtil.checkPassword(raw, hashed));
    }

    // Password sai → verify phải trả false
    @Test
    void wrongPasswordFailsVerification() {
        String hashed = SecurityUtil.hashPassword("correct");
        assertFalse(SecurityUtil.checkPassword("wrong", hashed));
    }

    // Cùng 1 password → BCrypt phải tạo hash khác nhau (vì salt ngẫu nhiên)
    @Test
    void sameInputProducesDifferentHashes() {
        String raw = "samepassword";
        String h1 = SecurityUtil.hashPassword(raw);
        String h2 = SecurityUtil.hashPassword(raw);
        assertNotEquals(h1, h2, "BCrypt dùng salt ngẫu nhiên");
    }
}
