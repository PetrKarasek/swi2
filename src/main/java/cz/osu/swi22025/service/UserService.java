package cz.osu.swi22025.service;

import cz.osu.swi22025.config.RabbitMQConfig;
import cz.osu.swi22025.model.ChatRoomRepository;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.MessageRepository;
import cz.osu.swi22025.model.ChatUser;
import cz.osu.swi22025.model.json.SignupForm;
import cz.osu.swi22025.model.json.UserToken;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import cz.osu.swi22025.model.json.UserProfileDto;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository; // (teď nevyužito, nechávám)
    private final RabbitMQConfig rabbitMQConfig;
    private final RabbitAdmin rabbitAdmin;

    // musí odpovídat souborům v src/main/resources/static/avatars
    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "cat.png", "female.png", "kapuce.png", "male.png", "robot.png"
    );

    public ResponseEntity<String> signup(SignupForm userDTO) {
        try {
            if (userDTO == null || userDTO.getUsername() == null || userDTO.getPassword() == null) {
                return new ResponseEntity<>("Missing username/password", HttpStatus.BAD_REQUEST);
            }

            String username = userDTO.getUsername().trim();
            String password = userDTO.getPassword().trim();

            if (username.isEmpty() || password.isEmpty()) {
                return new ResponseEntity<>("Missing username/password", HttpStatus.BAD_REQUEST);
            }

            if (userRepository.existsByUsernameIgnoreCase(username)) {
                return new ResponseEntity<>("Username is already taken", HttpStatus.CONFLICT);
            }

            ChatUser user = new ChatUser();
            user.setUsername(username);
            user.setPassword(password);
            // Password encryption (BCrypt, salt, ...)

            user.addRoom(chatRoomRepository.findByChatNameIgnoreCase("Public"));
            ChatUser savedUser = userRepository.save(user);

            // ✅ zajisti queue i binding
            ensureUserQueue(savedUser.getUserId());

            return new ResponseEntity<>("User registered successfully", HttpStatus.OK);

        } catch (DataIntegrityViolationException dup) {
            // ✅ kdyby se to střetlo na DB unique constraintu
            return new ResponseEntity<>("Username is already taken", HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>("Signup failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> authenticate(SignupForm userCredentials) {
        try {
            if (userCredentials == null || userCredentials.getUsername() == null || userCredentials.getPassword() == null) {
                return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }

            String username = userCredentials.getUsername().trim();
            String password = userCredentials.getPassword().trim();

            ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
            if (user == null) {
                return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }

            if (!user.getPassword().equals(password)) {
                return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }

            // ✅ při loginu taky zajisti queue
            ensureUserQueue(user.getUserId());

            UserToken userToken = new UserToken(user.getUserId(), user.getUsername());
            return new ResponseEntity<>(userToken, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void ensureUserQueue(UUID userId) {
        if (userId == null) return;

        String queueName = "chatroom.queue." + userId;

        // pokud neexistuje, vytvoř + bindni na exchange
        if (rabbitAdmin.getQueueInfo(queueName) == null) {
            Queue q = rabbitMQConfig.createUserQueue(userId.toString());

            rabbitAdmin.declareExchange(rabbitMQConfig.chatroomExchange());
            rabbitAdmin.declareQueue(q);
            rabbitAdmin.declareBinding(
                    rabbitMQConfig.bindUserQueue(q, rabbitMQConfig.chatroomExchange())
            );
        }
    }

    public UserProfileDto getProfileByUsername(String username) {
        ChatUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = "/avatars/cat.png"; // default
        }

        // 🚑 migrace starých (špatných) názvů z desktopu: girl/hoodie/man -> female/kapuce/male
        avatarUrl = avatarUrl
                .replace("/avatars/girl.png", "/avatars/female.png")
                .replace("/avatars/hoodie.png", "/avatars/kapuce.png")
                .replace("/avatars/man.png", "/avatars/male.png");

        return new UserProfileDto(user.getUsername(), avatarUrl);
    }

    public UserProfileDto updateAvatar(UUID userId, String avatarFile) {
        if (avatarFile == null || avatarFile.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Avatar file is missing");
        }

        // zkus převést historické názvy na nové
        if ("girl.png".equalsIgnoreCase(avatarFile)) avatarFile = "female.png";
        if ("hoodie.png".equalsIgnoreCase(avatarFile)) avatarFile = "kapuce.png";
        if ("man.png".equalsIgnoreCase(avatarFile)) avatarFile = "male.png";

        if (!ALLOWED_AVATARS.contains(avatarFile)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid avatar file");
        }

        ChatUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        user.setAvatarUrl("/avatars/" + avatarFile);
        ChatUser saved = userRepository.save(user);

        return new UserProfileDto(saved.getUsername(), saved.getAvatarUrl());
    }
}
