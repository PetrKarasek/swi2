package cz.osu.swi22025.model.json;

import java.util.UUID;

public class UserToken {

    private UUID userId;
    private String username;

    // ✅ POVINNÉ pro Jackson
    public UserToken() {
    }

    public UserToken(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
