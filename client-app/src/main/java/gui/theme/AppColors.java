package gui.theme;

import java.awt.Color;

public class AppColors {
    // Backgrounds — navy/charcoal palette (theo mockup CRM, tông xanh-đen)
    public static final Color BG_PRIMARY = Color.decode("#1A1E27");   // Main Chat Area
    public static final Color BG_SECONDARY = Color.decode("#151921"); // Channel/Friend sidebar + panel phải
    public static final Color BG_TERTIARY = Color.decode("#0E1116");  // Server rail / Input boxes (sâu nhất)
    public static final Color BG_HOVER = Color.decode("#232A38");     // Sidebar hover
    public static final Color BG_ACTIVE = Color.decode("#2B3344");    // Active/selected item
    public static final Color BG_FLOATING = Color.decode("#1D222D");  // Floating cards / tooltips / toast
    public static final Color BG_MESSAGE_HOVER = new Color(0xFF, 0xFF, 0xFF, 0x0D); // Hover sáng nhẹ trên nền tối

    // Separator
    public static final Color SEPARATOR = Color.decode("#272E3B");

    // Text Colors
    public static final Color TEXT_NORMAL = Color.decode("#D7DBE2");
    public static final Color TEXT_MUTED = Color.decode("#8B92A0");
    public static final Color TEXT_WHITE = Color.WHITE;
    public static final Color TEXT_LINK = Color.decode("#4DA6FF");
    public static final Color TEXT_HEADER = Color.decode("#F1F3F7");

    // Brand Colors — accent xanh-tím
    public static final Color BRAND_PRIMARY = Color.decode("#5B6CFF");
    public static final Color BRAND_HOVER = Color.decode("#4856E6");
    public static final Color BRAND_LINK = Color.decode("#4DA6FF");

    // Semantic
    public static final Color SUCCESS = Color.decode("#2FB36B");
    public static final Color DANGER = Color.decode("#F0484F");
    public static final Color WARNING = Color.decode("#F0B233");

    // Message Highlight — translucent brand (xanh-tím)
    public static final Color MSG_HIGHLIGHT_BG = new Color(0x5B, 0x6C, 0xFF, 0x1F);
    public static final Color MSG_HIGHLIGHT_BORDER = Color.decode("#5B6CFF");

    // Status Dots
    public static final Color STATUS_ONLINE = Color.decode("#2FB36B");
    public static final Color STATUS_IDLE = Color.decode("#F0B233");
    public static final Color STATUS_OFFLINE = Color.decode("#6B7280");
    public static final Color STATUS_DND = Color.decode("#F0484F");

    // Avatar Gradient Palette — unique colors per initial letter
    private static final Color[] AVATAR_COLORS = {
            Color.decode("#5865F2"), // Blurple
            Color.decode("#EB459E"), // Fuchsia
            Color.decode("#57F287"), // Green
            Color.decode("#FEE75C"), // Yellow
            Color.decode("#ED4245"), // Red
            Color.decode("#3BA55C"), // Emerald
            Color.decode("#E67E22"), // Orange
    };

    /** Get a consistent avatar background color based on username */
    public static Color avatarColorFor(String name) {
        if (name == null || name.isEmpty()) return BRAND_PRIMARY;
        int idx = Math.abs(name.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[idx];
    }
}