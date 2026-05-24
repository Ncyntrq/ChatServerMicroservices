package gui.theme;

import java.awt.*;

/**
 * Centralized font definitions.
 * Uses system fonts that handle Vietnamese and emoji well.
 */
public final class AppFonts {

    // Preferred font families (in order of preference)
    private static final String PRIMARY_FAMILY = findAvailableFont(
            "Segoe UI", "Noto Sans", ".SF NS", "Helvetica Neue", "SansSerif"
    );
    private static final String EMOJI_FAMILY = findAvailableFont(
            "Segoe UI Emoji", "Noto Color Emoji", "Apple Color Emoji", "SansSerif"
    );

    // Heading fonts
    public static final Font HEADING_LG = new Font(PRIMARY_FAMILY, Font.BOLD, 20);
    public static final Font HEADING_MD = new Font(PRIMARY_FAMILY, Font.BOLD, 16);
    public static final Font HEADING_SM = new Font(PRIMARY_FAMILY, Font.BOLD, 13);

    // Body fonts
    public static final Font BODY = new Font(PRIMARY_FAMILY, Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(PRIMARY_FAMILY, Font.BOLD, 14);
    public static final Font BODY_SM = new Font(PRIMARY_FAMILY, Font.PLAIN, 13);

    // Caption / Meta
    public static final Font CAPTION = new Font(PRIMARY_FAMILY, Font.PLAIN, 12);
    public static final Font CAPTION_BOLD = new Font(PRIMARY_FAMILY, Font.BOLD, 11);
    public static final Font TINY = new Font(PRIMARY_FAMILY, Font.PLAIN, 10);

    // Emoji
    public static final Font EMOJI = new Font(EMOJI_FAMILY, Font.PLAIN, 18);
    public static final Font EMOJI_SM = new Font(EMOJI_FAMILY, Font.PLAIN, 14);

    // Avatar
    public static final Font AVATAR_INITIAL = new Font(PRIMARY_FAMILY, Font.BOLD, 17);
    public static final Font AVATAR_INITIAL_SM = new Font(PRIMARY_FAMILY, Font.BOLD, 13);

    private AppFonts() {}

    /** Find the first available font from a list of preferred families */
    private static String findAvailableFont(String... families) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(available));
        for (String f : families) {
            if (set.contains(f)) return f;
        }
        return "SansSerif";
    }
}
