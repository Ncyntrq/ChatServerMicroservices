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

    private static final Map<String, java.util.List<java.util.function.Consumer<Image>>> loadingUrls = new ConcurrentHashMap<>();

    public static void loadAsync(String url, int targetSize, java.util.function.Consumer<Image> callback) {
        Image cached = cache.get(url);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        synchronized (loadingUrls) {
            java.util.List<java.util.function.Consumer<Image>> callbacks = loadingUrls.get(url);
            if (callbacks != null) {
                callbacks.add(callback);
                return;
            }
            callbacks = new java.util.ArrayList<>();
            callbacks.add(callback);
            loadingUrls.put(url, callbacks);
        }

        new Thread(() -> {
            Image resultImg = null;
            try {
                String token = network.SessionManager.get().getAccessToken();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + token)
                        .GET().build();
                java.net.http.HttpResponse<byte[]> resp = network.HttpClientHolder.get().send(req, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    Image downloadedImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(resp.body()));
                    if (downloadedImage != null && targetSize > 0) {
                        resultImg = downloadedImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
                    } else {
                        resultImg = downloadedImage;
                    }
                    if (resultImg != null) {
                        cache.put(url, resultImg);
                    }
                }
            } catch (Exception e) {}

            final Image finalResult = resultImg;
            javax.swing.SwingUtilities.invokeLater(() -> {
                java.util.List<java.util.function.Consumer<Image>> callbacks;
                synchronized (loadingUrls) {
                    callbacks = loadingUrls.remove(url);
                }
                if (callbacks != null) {
                    for (java.util.function.Consumer<Image> cb : callbacks) {
                        cb.accept(finalResult);
                    }
                }
            });
        }).start();
    }
}
