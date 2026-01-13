package cz.osu.swi22025.model.json;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private String username;
    private String avatarUrl; // např. "/avatars/cat.png"
}
