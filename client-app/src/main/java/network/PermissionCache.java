package network;

/**
 * Cache quyền (permission bitmask) của user hiện tại trong server đang mở.
 * Singleton để UI truy vấn nhanh khi dựng nút, không phải gọi API mỗi lần render.
 *
 * Các hằng số BIT phải khớp CHÍNH XÁC với enum Permission bên role-service.
 * Schema v2 (10 bit):
 *   READ_MESSAGES=1, MANAGE_MESSAGES=2, MANAGE_CHANNEL=4, KICK_MEMBER=8,
 *   BAN_MEMBER=16, MANAGE_ROLES=32, ADMIN=64, MANAGE_SERVER=128,
 *   CREATE_INVITE=256, MANAGE_NICKNAMES=512, ALL=1023
 */
public final class PermissionCache {

    // ── Bit constants — khớp với Permission enum ─────────────────────────
    public static final int READ_MESSAGES    = 1;    // 1 << 0
    public static final int MANAGE_MESSAGES  = 2;    // 1 << 1
    public static final int MANAGE_CHANNEL   = 4;    // 1 << 2
    public static final int KICK_MEMBER      = 8;    // 1 << 3
    public static final int BAN_MEMBER       = 16;   // 1 << 4
    public static final int MANAGE_ROLES     = 32;   // 1 << 5
    public static final int ADMIN            = 64;   // 1 << 6
    public static final int MANAGE_SERVER    = 128;  // 1 << 7
    public static final int CREATE_INVITE    = 256;  // 1 << 8
    public static final int MANAGE_NICKNAMES = 512;  // 1 << 9

    // ─────────────────────────────────────────────────────────────────────

    private static final PermissionCache INSTANCE = new PermissionCache();

    private volatile long serverId = -1;
    private volatile int bitmask = 0;

    private PermissionCache() {}

    public static PermissionCache get() { return INSTANCE; }

    /** Lưu quyền cho server đang mở. */
    public void set(long serverId, int bitmask) {
        this.serverId = serverId;
        this.bitmask = bitmask;
    }

    /** Xóa quyền (về Home/DM — không thuộc server nào). */
    public void clear() {
        this.serverId = -1;
        this.bitmask = 0;
    }

    public long serverId() { return serverId; }

    public int getBitmask() { return bitmask; }

    /**
     * Kiểm tra có quyền {@code bit} hay không.
     * ADMIN (64) bao trùm mọi quyền khác.
     */
    public boolean can(int bit) {
        return (bitmask & bit) != 0 || (bitmask & ADMIN) != 0;
    }
}
