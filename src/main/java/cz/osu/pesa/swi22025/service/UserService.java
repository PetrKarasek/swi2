package cz.osu.pesa.swi22025.service;

import cz.osu.pesa.swi22025.model.UserRepository;
import cz.osu.pesa.swi22025.model.json.SignupForm;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public ResponseEntity<String> signup(SignupForm userDTO) {
        // TODO
    }

    public ResponseEntity<Object> authenticate(SignupForm userCredentials) {
        // TODO
    }
}
