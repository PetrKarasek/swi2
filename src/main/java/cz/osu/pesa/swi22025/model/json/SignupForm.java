package cz.osu.pesa.swi22025.model.json;

import cz.osu.pesa.swi22025.model.db.ChatUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupForm {
    private String username;
    private String password;
}
