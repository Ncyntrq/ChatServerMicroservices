package com.chatsever.role.model;

/**
 * Hệ thống permission bitmask (R3).
 * Mỗi permission là 1 bit, cho phép kết hợp linh hoạt.
 */
public enum Permission {
    SEND_MESSAGE    (1 << 0),  // 1
    READ_MESSAGES   (1 << 1),  // 2
    MANAGE_MESSAGES (1 << 2),  // 4
    MANAGE_CHANNEL  (1 << 3),  // 8
    KICK_MEMBER     (1 << 4),  // 16
    BAN_MEMBER      (1 << 5),  // 32
    MANAGE_ROLES    (1 << 6),  // 64
    ADMIN           (1 << 7);  // 128

    private final int bit;

    Permission(int bit) { this.bit = bit; }

    public int getBit() { return bit; }

    /** Tất cả quyền (Owner) */
    public static final int ALL = (1 << 8) - 1; // 255

    /** Quyền mặc định của Member */
    public static final int MEMBER_DEFAULT = SEND_MESSAGE.bit | READ_MESSAGES.bit; // 3

    /** Quyền mặc định của Moderator */
    public static final int MODERATOR_DEFAULT = MEMBER_DEFAULT | KICK_MEMBER.bit | MANAGE_MESSAGES.bit; // 23

    /** Quyền mặc định của Admin */
    public static final int ADMIN_DEFAULT = MODERATOR_DEFAULT | MANAGE_CHANNEL.bit | BAN_MEMBER.bit | MANAGE_ROLES.bit; // 127

    /** Kiểm tra xem bitmask có chứa permission này không */
    public static boolean hasPermission(int bitmask, Permission perm) {
        return (bitmask & perm.bit) != 0;
    }

    /** Parse danh sách permission names → bitmask */
    public static int fromNames(String permissionNames) {
        if (permissionNames == null || permissionNames.isBlank()) return MEMBER_DEFAULT;
        int mask = 0;
        for (String name : permissionNames.split(",")) {
            try {
                mask |= Permission.valueOf(name.trim().toUpperCase()).bit;
            } catch (IllegalArgumentException ignored) {
                // Bỏ qua permission không hợp lệ
            }
        }
        return mask;
    }

    /** Chuyển bitmask → danh sách tên permission */
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
