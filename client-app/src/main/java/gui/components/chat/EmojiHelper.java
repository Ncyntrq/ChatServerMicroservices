package gui.components.chat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmojiHelper {
    private static final String BASE_URL = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/";

    public static final Map<String, String> EMOJIS = new LinkedHashMap<>();
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> IMG_CACHE = new ConcurrentHashMap<>();

    static {
        EMOJIS.put(":smile:", "1f642");
        EMOJIS.put(":joy:", "1f602");
        EMOJIS.put(":rofl:", "1f923");
        EMOJIS.put(":blush:", "1f60a");
        EMOJIS.put(":heart_eyes:", "1f60d");
        EMOJIS.put(":kissing_heart:", "1f618");
        EMOJIS.put(":sob:", "1f62d");
        EMOJIS.put(":triumph:", "1f624");
        EMOJIS.put(":rage:", "1f621");
        EMOJIS.put(":thumbsup:", "1f44d");
        EMOJIS.put(":thumbsdown:", "1f44e");
        EMOJIS.put(":heart:", "2764");
        EMOJIS.put(":fire:", "1f525");
        EMOJIS.put(":sparkles:", "2728");
        EMOJIS.put(":tada:", "1f389");

        // Preload emojis in background thread
        new Thread(() -> {
            for (Map.Entry<String, String> entry : EMOJIS.entrySet()) {
                getEmojiIcon(entry.getValue(), 20); // Preload size 20 for chat
                getEmojiIcon(entry.getValue(), 24); // Preload size 24 for picker
            }
        }).start();
    }

    public static ImageIcon getEmojiIcon(String twemojiCode, int size) {
        String key = twemojiCode + "_" + size;
        ImageIcon cached = CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage img = getEmojiImage(twemojiCode, size);
        if (img != null) {
            ImageIcon icon = new ImageIcon(img);
            CACHE.put(key, icon);
            return icon;
        }
        return null;
    }

    /** Tải + scale ảnh Twemoji (cache theo code+size). Trả null nếu offline / không có mã. */
    public static BufferedImage getEmojiImage(String twemojiCode, int size) {
        String key = twemojiCode + "_" + size;
        BufferedImage cached = IMG_CACHE.get(key);
        if (cached != null) return cached;
        try {
            URL url = new URL(BASE_URL + twemojiCode + ".png");
            BufferedImage img = ImageIO.read(url);
            if (img != null) {
                BufferedImage scaled = gui.utils.ImageUtils.highQualityScale(img, size, size);
                IMG_CACHE.put(key, scaled);
                return scaled;
            }
        } catch (Exception e) {
            // Silently fail, it will fall back to raw text/emoji
        }
        return null;
    }

    /** Mã Twemoji của 1 chuỗi emoji: codepoint hex nối bằng '-', bỏ variation selector (FE0F). */
    public static String twemojiCode(String emoji) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < emoji.length()) {
            int cp = emoji.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 0xFE0F) continue; // Twemoji không gắn FE0F vào tên file (trừ keycap)
            if (sb.length() > 0) sb.append('-');
            sb.append(Integer.toHexString(cp));
        }
        return sb.toString();
    }

    /** Heuristic: chuỗi bắt đầu bằng codepoint vùng symbol/emoji (loại trừ chữ, số, '#'…). */
    public static boolean isEmoji(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.codePointAt(0) >= 0x2190; // dưới mức này là ASCII/chữ Latin → giữ nguyên text
    }

    /**
     * Icon Twemoji cho 1 ký tự emoji (đồng nhất mọi OS), KHÔNG chặn EDT: nếu chưa có trong cache
     * thì tải nền rồi gọi {@code onReady} trên EDT. Null-callback nếu không phải emoji / offline.
     * Dùng ở constructor/EDT để tránh đơ UI khi fetch từ CDN.
     */
    public static void iconForCharAsync(String emoji, int size, java.util.function.Consumer<ImageIcon> onReady) {
        if (!isEmoji(emoji)) return;
        String key = twemojiCode(emoji) + "_" + size;
        ImageIcon cached = CACHE.get(key);
        if (cached != null) { onReady.accept(cached); return; }
        new Thread(() -> {
            ImageIcon icon = getEmojiIcon(twemojiCode(emoji), size);
            if (icon != null) SwingUtilities.invokeLater(() -> onReady.accept(icon));
        }, "emoji-load").start();
    }

    /** Ảnh Twemoji đã nằm trong cache (không fetch) — an toàn gọi trong paintComponent. */
    public static BufferedImage cachedImageForChar(String emoji, int size) {
        if (!isEmoji(emoji)) return null;
        return IMG_CACHE.get(twemojiCode(emoji) + "_" + size);
    }

    /** Nạp nền ảnh Twemoji cho 1 ký tự rồi chạy {@code onReady} trên EDT (để repaint). */
    public static void prefetchChar(String emoji, int size, Runnable onReady) {
        if (!isEmoji(emoji)) return;
        if (cachedImageForChar(emoji, size) != null) { onReady.run(); return; }
        new Thread(() -> {
            if (getEmojiImage(twemojiCode(emoji), size) != null) SwingUtilities.invokeLater(onReady);
        }, "emoji-prefetch").start();
    }

    public static void renderTextWithEmojis(JTextPane textPane, String text) {
        textPane.setText("");
        if (text == null || text.isEmpty()) return;

        String remaining = text;
        while (!remaining.isEmpty()) {
            int firstMatchIdx = -1;
            String foundShortcode = null;
            String foundTwemoji = null;

            for (Map.Entry<String, String> entry : EMOJIS.entrySet()) {
                int idx = remaining.indexOf(entry.getKey());
                if (idx != -1) {
                    if (firstMatchIdx == -1 || idx < firstMatchIdx) {
                        firstMatchIdx = idx;
                        foundShortcode = entry.getKey();
                        foundTwemoji = entry.getValue();
                    }
                }
            }

            try {
                if (firstMatchIdx != -1 && foundShortcode != null && foundTwemoji != null) {
                    // Append text before the shortcode
                    if (firstMatchIdx > 0) {
                        textPane.getDocument().insertString(textPane.getDocument().getLength(), remaining.substring(0, firstMatchIdx), null);
                    }
                    // Append icon
                    ImageIcon icon = getEmojiIcon(foundTwemoji, 20);
                    if (icon != null) {
                        textPane.setCaretPosition(textPane.getDocument().getLength());
                        textPane.insertIcon(icon);
                    } else {
                        textPane.getDocument().insertString(textPane.getDocument().getLength(), foundShortcode, null);
                    }
                    remaining = remaining.substring(firstMatchIdx + foundShortcode.length());
                } else {
                    // No more emojis, append rest
                    textPane.getDocument().insertString(textPane.getDocument().getLength(), remaining, null);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        hideCaret(textPane); // setCaretPosition khi chèn icon có thể bật con trỏ lại — ẩn đi
    }

    /** Ẩn con trỏ soạn thảo của pane chỉ-đọc (sau khi insertIcon gọi setCaretPosition). */
    private static void hideCaret(JTextPane pane) {
        if (pane.getCaret() != null) pane.getCaret().setVisible(false);
    }
}
