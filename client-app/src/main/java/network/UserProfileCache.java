package network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserProfileCache {
    private static final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public static Map<String, Object> get(String username) {
        return cache.get(username);
    }

    public static void put(String username, Map<String, Object> profile) {
        cache.put(username, profile);
    }

    public static void clear() {
        cache.clear();
    }
    
    public static void clear(String username) {
        cache.remove(username);
    }
}
