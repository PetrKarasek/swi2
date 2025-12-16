package cz.osu.swi22025.model.json;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserToken {
    private UUID userId;
    private String username;
}
