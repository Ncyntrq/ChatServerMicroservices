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
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        try {
            URL url = new URL(BASE_URL + twemojiCode + ".png");
            BufferedImage img = ImageIO.read(url);
            if (img != null) {
                Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                CACHE.put(key, icon);
                return icon;
            }
        } catch (Exception e) {
            // Silently fail, it will fall back
        }
        return null;
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
    }
}
