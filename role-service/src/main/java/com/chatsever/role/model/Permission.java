package com.chatsever.role.model;

/**
 * Hệ thống permission bitmask (R3).
 * Mỗi permission là 1 bit, cho phép kết hợp linh hoạt qua toán tử OR.
 *
 * Permissions đã loại bỏ: voice, camera, call, embed links, attach files,
 * read history, mention everyone, manage emojis/webhooks/expressions,
 * threads, change_nickname, send_message (quá cơ bản, mặc định ai cũng có).
 */
public enum Permission {

    // ── Kênh văn bản ──────────────────────────────────────────────────
    READ_MESSAGES   (1 << 0),  // 1   — Đọc kênh / Xem kênh
    MANAGE_MESSAGES (1 << 1),  // 2   — Quản lý tin nhắn (xóa, ghim)

    // ── Cấu trúc server ──────────────────────────────────────────────
    MANAGE_CHANNEL  (1 << 2),  // 4   — Quản lý kênh (tạo / sửa / xóa)
    KICK_MEMBER     (1 << 3),  // 8   — Đá thành viên
    BAN_MEMBER      (1 << 4),  // 16  — Cấm thành viên vĩnh viễn
    MANAGE_ROLES    (1 << 5),  // 32  — Quản lý vai trò
    ADMIN           (1 << 6),  // 64  — Quản trị viên (bỏ qua mọi quyền khác)
    MANAGE_SERVER   (1 << 7),  // 128 — Quản lý máy chủ

    // ── Thành viên ────────────────────────────────────────────────────
    CREATE_INVITE   (1 << 8),  // 256 — Tạo lời mời
    MANAGE_NICKNAMES(1 << 9);  // 512 — Quản lý biệt danh người khác

    private final int bit;

    Permission(int bit) { this.bit = bit; }

    public int getBit() { return bit; }

    // ── Preset bitmask ────────────────────────────────────────────────

    /** Tất cả quyền (dành cho Owner) */
    public static final int ALL = (1 << 10) - 1; // 1023

    /** Quyền mặc định của Member: chỉ đọc kênh */
    public static final int MEMBER_DEFAULT = READ_MESSAGES.bit; // 1

    /** Quyền mặc định của Moderator */
    public static final int MODERATOR_DEFAULT =
            MEMBER_DEFAULT | MANAGE_MESSAGES.bit | KICK_MEMBER.bit; // 11

    /** Quyền mặc định của Admin */
    public static final int ADMIN_DEFAULT =
            MODERATOR_DEFAULT | MANAGE_CHANNEL.bit | BAN_MEMBER.bit
            | MANAGE_ROLES.bit | MANAGE_SERVER.bit; // 191

    // ── Utility ───────────────────────────────────────────────────────

    /** Kiểm tra xem bitmask có chứa permission này không. */
    public static boolean hasPermission(int bitmask, Permission perm) {
        return (bitmask & perm.bit) != 0;
    }

    /**
     * Parse danh sách tên permission phân cách bởi dấu phẩy → bitmask.
     * Tên không hợp lệ bị bỏ qua (không throw exception).
     */
    public static int fromNames(String permissionNames) {
        if (permissionNames == null || permissionNames.isBlank()) return MEMBER_DEFAULT;
        int mask = 0;
        for (String name : permissionNames.split(",")) {
            try {
                mask |= Permission.valueOf(name.trim().toUpperCase()).bit;
            } catch (IllegalArgumentException ignored) {}
        }
        return mask == 0 ? MEMBER_DEFAULT : mask;
    }

    /** Chuyển bitmask → danh sách tên permission ngăn cách bởi dấu phẩy. */
    public static String toNames(int bitmask) {
        StringBuilder sb = new StringBuilder();
        for (Permission p : values()) {
            if ((bitmask & p.bit) != 0) {
                if (sb.length() > 0) sb.append(",");
                sb.append(p.name());
            }
        }
        return sb.toString();
    }
}
