package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.json.SignupForm;
import cz.osu.swi22025.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import cz.osu.swi22025.model.json.AvatarUpdateRequest;
import cz.osu.swi22025.model.json.UserProfileDto;

import java.util.UUID;

@RestController
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public UserController(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/users/by-username/{username}")
    public UserProfileDto getProfile(@PathVariable String username) {
        return userService.getProfileByUsername(username);
    }

    @PutMapping("/api/users/{userId}/avatar")
    public UserProfileDto updateAvatar(
            @PathVariable UUID userId,
            @RequestBody AvatarUpdateRequest request
    ) {
        return userService.updateAvatar(userId, request.getAvatarFile());
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupForm userDTO) {
        return userService.signup(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> authenticate(@RequestBody SignupForm userCredentials) {
        return userService.authenticate(userCredentials);
    }

    @PostMapping("/send")
    public void send() {
        PayloadMessage message = new PayloadMessage();
        message.setContent("Ahoj");
        message.setSenderName("Radim");
        message.setReceiverChatRoomId("1");
        String destination = "/chatroom/" + message.getReceiverChatRoomId();
        messagingTemplate.convertAndSend(destination, message);
    }















}
