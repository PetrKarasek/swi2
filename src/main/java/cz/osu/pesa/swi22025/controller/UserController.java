package cz.osu.pesa.swi22025.controller;

import cz.osu.pesa.swi22025.model.json.SignupForm;
import cz.osu.pesa.swi22025.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public UserController(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupForm userDTO) {
        return userService.signup(userDTO);
    }

    @PostMapping("login")
    public ResponseEntity<Object> authenticate(@RequestBody SignupForm userCredentials) {
        return userService.authenticate(userCredentials);
    }
}
