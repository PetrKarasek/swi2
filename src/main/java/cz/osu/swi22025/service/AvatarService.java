package cz.osu.swi22025.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvatarService {

    // Ukládáme RELATIVNÍ cesty (to je klíč sjednocení web + desktop)
    private static final List<String> DEFAULT_AVATARS = List.of(
            "/avatars/cat.png",
            "/avatars/female.png",
            "/avatars/kapuce.png",
            "/avatars/male.png",
            "/avatars/robot.png"
    );

    public List<String> getAvailableAvatars() {
        return DEFAULT_AVATARS;
    }

    // === Backwards-compatible method names used elsewhere in the project ===
    // (ChatController historically called getAllAvatars/getAvatarUrl)
    public List<String> getAllAvatars() {
        return getAvailableAvatars();
    }

    public String getDefaultAvatar() {
        return DEFAULT_AVATARS.get(0);
    }

    /** Normalizuje uloženou hodnotu avatara do tvaru "/avatars/xxx.png" */
    public String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return getDefaultAvatar();

        // Když je v DB uložené něco jako "http://localhost:8081/avatars/robot.png"
        int idx = avatarUrl.indexOf("/avatars/");
        if (idx >= 0) {
            return avatarUrl.substring(idx);
        }

        // Když je v DB uložené jen "robot.png"
        if (!avatarUrl.startsWith("/")) {
            return "/avatars/" + avatarUrl;
        }

        return avatarUrl;
    }

    public String avatarByIndex(int index) {
        if (index < 0 || index >= DEFAULT_AVATARS.size()) return getDefaultAvatar();
        return DEFAULT_AVATARS.get(index);
    }

    // Backwards-compatible alias
    public String getAvatarUrl(int index) {
        return avatarByIndex(index);
    }
}
