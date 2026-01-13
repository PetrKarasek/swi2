package cz.osu.swi22025.desktop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AvatarCache {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String username) {
        return cache.get(username.toLowerCase());
    }

    public void put(String username, String avatarUrl) {
        if (username == null) return;
        cache.put(username.toLowerCase(), avatarUrl);
    }

    public void clear() {
        cache.clear();
    }
}
