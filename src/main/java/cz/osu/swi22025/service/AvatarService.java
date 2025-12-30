package cz.osu.swi22025.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AvatarService {

    private static final List<String> DEFAULT_AVATARS = Arrays.asList(
        "cat.png",
        "female.png", 
        "kapuce.png",
        "male.png",
        "robot.png"
    );

    public List<String> getAllAvatars() {
        return DEFAULT_AVATARS.stream()
            .map(avatar -> "http://localhost:8081/avatars/" + avatar)
            .toList();
    }

    public String getAvatarUrl(int index) {
        if (index >= 0 && index < DEFAULT_AVATARS.size()) {
            return "http://localhost:8081/avatars/" + DEFAULT_AVATARS.get(index);
        }
        return "http://localhost:8081/avatars/cat.png"; // Default to cat.png
    }

    public boolean isValidAvatar(String avatarUrl) {
        return getAllAvatars().contains(avatarUrl);
    }
}
