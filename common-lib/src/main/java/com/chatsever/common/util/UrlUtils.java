package com.chatsever.common.util;

public class UrlUtils {
    /**
     * Chuẩn hóa & kiểm tra URL media (ảnh, icon) trước khi lưu DB (defense-in-depth).
     * - Nếu là URL tuyệt đối / protocol-relative (chứa "://" hoặc bắt đầu "//"),
     *   chỉ giữ lại phần path → vô hiệu hóa host do attacker kiểm soát.
     * - Chỉ chấp nhận path nội bộ theo danh sách allowedPrefixes.
     * Trả về path tương đối an toàn, hoặc null nếu không hợp lệ.
     */
    public static String toSafeRelativeMediaUrl(String raw, String... allowedPrefixes) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.contains("://") || v.startsWith("//")) {
            try {
                String path = java.net.URI.create(v).getPath();
                v = path == null ? "" : path;
            } catch (Exception e) {
                return null;
            }
        }
        for (String prefix : allowedPrefixes) {
            if (v.startsWith(prefix)) return v;
        }
        return null;
    }
}
