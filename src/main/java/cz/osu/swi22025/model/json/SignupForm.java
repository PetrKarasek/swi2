package cz.osu.pesa.swi22025.model.json;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupForm {
    private String username;
    private String password;
}
