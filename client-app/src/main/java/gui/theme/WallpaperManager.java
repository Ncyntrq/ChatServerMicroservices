package gui.theme;

import java.util.Random;
import java.util.prefs.Preferences;

/**
 * Quản lý hoạ tiết nền chat do người dùng chọn. Lưu/đọc qua {@link Preferences}.
 * Giá trị lưu: "NONE", "RANDOM", hoặc id pattern ("cats" / "starwars" / "sweets").
 * Mirror cấu trúc {@link UiScale} / {@link ThemeManager}.
 */
public final class WallpaperManager {

    public static final String NONE = "NONE";
    public static final String RANDOM = "RANDOM";
    /** Ảnh nền tùy chỉnh do user chọn từ máy (đường dẫn lưu ở PREF_IMAGE). */
    public static final String CUSTOM_IMAGE = "CUSTOM_IMAGE";
    /** Màu nền đặc do user chọn (RGB lưu ở PREF_COLOR). */
    public static final String CUSTOM_COLOR = "CUSTOM_COLOR";
    /** Các pattern có sẵn (khớp tên file resources/wallpapers/pattern-<id>-<theme>.png). */
    public static final String[] PATTERNS = { "cats", "starwars", "sweets" };

    private static final WallpaperManager INSTANCE = new WallpaperManager();
    private static final String PREF_KEY = "ui.wallpaper";
    private static final String PREF_IMAGE = "ui.wallpaper.imagePath";
    private static final String PREF_COLOR = "ui.wallpaper.color";

    private final Preferences prefs = Preferences.userNodeForPackage(WallpaperManager.class);
    private String selection;
    /** Pattern đã bốc ngẫu nhiên cho phiên hiện tại (cache để không nhảy mỗi lần repaint). */
    private String randomPick;

    private WallpaperManager() {
        this.selection = prefs.get(PREF_KEY, RANDOM);
    }

    public static WallpaperManager get() { return INSTANCE; }

    public String selection() { return selection; }

    public void set(String value) {
        this.selection = value;
        prefs.put(PREF_KEY, value);
        if (RANDOM.equals(value)) randomPick = null; // bốc lại lần sau
    }

    /** Đường dẫn file ảnh nền tùy chỉnh ("" nếu chưa chọn). */
    public String customImagePath() { return prefs.get(PREF_IMAGE, ""); }

    /** Đặt ảnh nền tùy chỉnh + chuyển sang chế độ CUSTOM_IMAGE. */
    public void setCustomImage(String path) {
        prefs.put(PREF_IMAGE, path != null ? path : "");
        set(CUSTOM_IMAGE);
    }

    /** Màu nền đặc đã lưu (mặc định BG_PRIMARY nếu chưa đặt). */
    public int customColor() { return prefs.getInt(PREF_COLOR, AppColors.BG_PRIMARY.getRGB()); }

    /** Đặt màu nền đặc + chuyển sang chế độ CUSTOM_COLOR. */
    public void setCustomColor(int rgb) {
        prefs.putInt(PREF_COLOR, rgb);
        set(CUSTOM_COLOR);
    }

    /**
     * Khoá nhận diện nền đang hiệu lực (cũng dùng làm cache key cho renderer):
     * null = tắt; "img:<path>" / "color:<rgb>" cho chế độ tùy chỉnh; còn lại là id pattern.
     */
    public String activePatternId() {
        if (NONE.equals(selection)) return null;
        if (CUSTOM_IMAGE.equals(selection)) return "img:" + customImagePath();
        if (CUSTOM_COLOR.equals(selection)) return "color:" + customColor();
        if (RANDOM.equals(selection)) {
            if (randomPick == null) {
                randomPick = PATTERNS[new Random().nextInt(PATTERNS.length)];
            }
            return randomPick;
        }
        return selection;
    }

    public boolean isEnabled() {
        return !NONE.equals(selection);
    }
}
