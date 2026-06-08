package network;

/**
 * Cache quyền (permission bitmask) của user hiện tại trong server đang mở.
 * Singleton (như {@link SessionManager}) để UI (vd ChatMessageItem) truy vấn
 * nhanh khi dựng nút, không phải gọi API mỗi lần render.
 */
public final class PermissionCache {

    // Bit quyền — khớp với enum phía backend (role-service)
    public static final int MANAGE_MESSAGES = 4;
    public static final int MANAGE_CHANNEL = 8;
    public static final int KICK_MEMBER = 16;
    public static final int MANAGE_ROLES = 64;
    public static final int ADMIN = 128;

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

    /** Có quyền {@code bit} hay không (ADMIN bao trùm mọi quyền). */
    public boolean can(int bit) {
        return (bitmask & bit) != 0 || (bitmask & ADMIN) != 0;
    }
}
