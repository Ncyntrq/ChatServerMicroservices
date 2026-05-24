package gui.theme;

import java.awt.Color;

public class AppColors {
    // Backgrounds — refined dark palette
    public static final Color BG_PRIMARY = Color.decode("#313338");   // Main Chat Area
    public static final Color BG_SECONDARY = Color.decode("#2B2D31"); // Channel sidebar
    public static final Color BG_TERTIARY = Color.decode("#1E1F22");  // Server sidebar / Input boxes
    public static final Color BG_HOVER = Color.decode("#393C41");     // Sidebar hover
    public static final Color BG_ACTIVE = Color.decode("#43444B");    // Active/selected item
    public static final Color BG_FLOATING = Color.decode("#232428");  // Floating cards / tooltips
    public static final Color BG_MESSAGE_HOVER = new Color(0x04, 0x04, 0x04, 0x0C); // Subtle message row hover

    // Separator
    public static final Color SEPARATOR = Color.decode("#3F4147");

    // Text Colors
    public static final Color TEXT_NORMAL = Color.decode("#DBDEE1");
    public static final Color TEXT_MUTED = Color.decode("#949BA4");
    public static final Color TEXT_WHITE = Color.WHITE;
    public static final Color TEXT_LINK = Color.decode("#00A8FC");
    public static final Color TEXT_HEADER = Color.decode("#F2F3F5");

    // Brand Colors
    public static final Color BRAND_PRIMARY = Color.decode("#5865F2"); // Blurple
    public static final Color BRAND_HOVER = Color.decode("#4752C4");   // Blurple hover
    public static final Color BRAND_LINK = Color.decode("#00A8FC");

    // Semantic
    public static final Color SUCCESS = Color.decode("#23A559");
    public static final Color DANGER = Color.decode("#F23F42");
    public static final Color WARNING = Color.decode("#F0B232");

    // Message Highlight
    public static final Color MSG_HIGHLIGHT_BG = new Color(0xF9, 0xA8, 0x26, 0x14); // Translucent amber
    public static final Color MSG_HIGHLIGHT_BORDER = Color.decode("#F0B232");

    // Status Dots
    public static final Color STATUS_ONLINE = Color.decode("#23A559");
    public static final Color STATUS_IDLE = Color.decode("#F0B232");
    public static final Color STATUS_OFFLINE = Color.decode("#80848E");
    public static final Color STATUS_DND = Color.decode("#F23F42");

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