package cz.osu.swi22025.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AvatarService {

    private static final List<String> DEFAULT_AVATARS = Arrays.asList(
        "http://localhost:8081/avatars/cat.png",
        "http://localhost:8081/avatars/female.png", 
        "http://localhost:8081/avatars/kapuce.png",
        "http://localhost:8081/avatars/male.png",
        "http://localhost:8081/avatars/robot.png"
    );

    public List<String> getAllAvatars() {
        return DEFAULT_AVATARS;
    }

    public String getAvatarUrl(int avatarIndex) {
        if (avatarIndex < 0 || avatarIndex >= DEFAULT_AVATARS.size()) {
            return DEFAULT_AVATARS.get(0);
        }
        return DEFAULT_AVATARS.get(avatarIndex);
    }

    public boolean isValidAvatar(String avatarUrl) {
        return DEFAULT_AVATARS.contains(avatarUrl);
    }
}
