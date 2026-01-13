package cz.osu.swi22025.model.json;

import lombok.Data;

@Data
public class AvatarUpdateRequest {
    private String avatarFile; // např. "cat.png"
}
