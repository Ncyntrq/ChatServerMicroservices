package gui.theme;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIManager;
import java.awt.*;

/**
 * Centralized font definitions.
 * Uses system fonts that handle Vietnamese and emoji well.
 *
 * Các Font KHÔNG còn là hằng {@code final}: chúng được dựng lại từ base size * {@link UiScale}
 * mỗi khi gọi {@link #rescale()} (sau khi người dùng zoom). Code vẫn gọi {@code AppFonts.BODY}
 * như trước — chỉ cần rebuild UI sau khi rescale để các component đọc lại Font mới.
 */
public final class AppFonts {

    // Preferred font families (in order of preference)
    private static final String PRIMARY_FAMILY = findAvailableFont(
            "Roboto", "Segoe UI", "Noto Sans", ".SF NS", "Helvetica Neue", "SansSerif"
    );
    private static final String EMOJI_FAMILY = findAvailableFont(
            "Segoe UI Emoji", "Noto Color Emoji", "Apple Color Emoji", "SansSerif"
    );

    // Base sizes (px @ scale 1.0) — nguồn duy nhất để tính lại khi zoom.
    private static final int SZ_HEADING_LG = 20, SZ_HEADING_MD = 16, SZ_HEADING_SM = 13;
    private static final int SZ_BODY = 14, SZ_BODY_BOLD = 14, SZ_BODY_SM = 13;
    private static final int SZ_CAPTION = 12, SZ_CAPTION_BOLD = 11, SZ_TINY = 10;
    private static final int SZ_EMOJI = 18, SZ_EMOJI_SM = 14;
    private static final int SZ_AVATAR = 17, SZ_AVATAR_SM = 13;

    // Heading fonts
    public static Font HEADING_LG, HEADING_MD, HEADING_SM;
    // Body fonts
    public static Font BODY, BODY_BOLD, BODY_ITALIC, BODY_SM;
    // Caption / Meta
    public static Font CAPTION, CAPTION_BOLD, TINY;
    // Emoji
    public static Font EMOJI, EMOJI_SM;
    // Avatar
    public static Font AVATAR_INITIAL, AVATAR_INITIAL_SM;

    static {
        rescale();
    }

    private AppFonts() {}

    /** Dựng lại toàn bộ Font theo hệ số zoom hiện hành ({@link UiScale}). */
    public static void rescale() {
        HEADING_LG = primary(Font.BOLD, SZ_HEADING_LG);
        HEADING_MD = primary(Font.BOLD, SZ_HEADING_MD);
        HEADING_SM = primary(Font.BOLD, SZ_HEADING_SM);

        BODY = primary(Font.PLAIN, SZ_BODY);
        BODY_BOLD = primary(Font.BOLD, SZ_BODY_BOLD);
        BODY_ITALIC = primary(Font.ITALIC, SZ_BODY);
        BODY_SM = primary(Font.PLAIN, SZ_BODY_SM);

        CAPTION = primary(Font.PLAIN, SZ_CAPTION);
        CAPTION_BOLD = primary(Font.BOLD, SZ_CAPTION_BOLD);
        TINY = primary(Font.PLAIN, SZ_TINY);

        EMOJI = new Font(EMOJI_FAMILY, Font.PLAIN, UiScale.get().scaled(SZ_EMOJI));
        EMOJI_SM = new Font(EMOJI_FAMILY, Font.PLAIN, UiScale.get().scaled(SZ_EMOJI_SM));

        AVATAR_INITIAL = primary(Font.BOLD, SZ_AVATAR);
        AVATAR_INITIAL_SM = primary(Font.BOLD, SZ_AVATAR_SM);
    }

    private static Font primary(int style, int baseSize) {
        return new Font(PRIMARY_FAMILY, style, UiScale.get().scaled(baseSize));
    }

    /**
     * Áp hệ số zoom hiện hành lên TOÀN bộ UI: dựng lại Font của AppFonts + đặt lại defaultFont của
     * FlatLaf (để FlatLaf scale padding/arc của component chuẩn) rồi refresh L&F.
     * Gọi lúc khởi động và sau mỗi lần người dùng zoom. Sau đó caller nên
     * {@code SwingUtilities.updateComponentTreeUI(frame)} để các component đọc lại Font.
     */
    public static void applyGlobalScale() {
        rescale();
        Font base = UIManager.getFont("defaultFont");
        // Luôn tính size từ base 13 * factor → không cộng dồn qua các lần zoom.
        String family = base != null ? base.getFamily() : PRIMARY_FAMILY;
        int style = base != null ? base.getStyle() : Font.PLAIN;
        UIManager.put("defaultFont", new Font(family, style, UiScale.get().scaled(13)));
        FlatLaf.updateUI();
    }

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
