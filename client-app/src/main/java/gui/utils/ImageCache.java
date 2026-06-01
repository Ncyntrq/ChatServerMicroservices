package gui.utils;

import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    public static Image get(String url) {
        return cache.get(url);
    }

    public static void put(String url, Image img) {
        cache.put(url, img);
    }

    public static void clear() {
        cache.clear();
    }
}
