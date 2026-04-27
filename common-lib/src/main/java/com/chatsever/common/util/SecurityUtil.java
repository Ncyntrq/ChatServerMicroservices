package com.chatsever.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility hash/verify mật khẩu bằng BCrypt.
 * BCryptPasswordEncoder thread-safe → dùng singleton OK.
 */
public final class SecurityUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SecurityUtil() {} // Không cho tạo instance

    /** Hash password → chuỗi BCrypt ($2a$10$...) để lưu DB */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password không được null");
        }
        return ENCODER.encode(rawPassword);
    }

    /** So sánh password plaintext với hash trong DB. Trả false nếu input null. */
    public static boolean checkPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}
