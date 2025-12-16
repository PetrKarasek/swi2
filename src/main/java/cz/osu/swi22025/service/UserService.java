package cz.osu.swi22025.service;

import cz.osu.swi22025.config.RabbitMQConfig;
import cz.osu.swi22025.model.ChatRoomRepository;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.db.ChatUser;
import cz.osu.swi22025.model.MessageRepository;
import cz.osu.swi22025.model.json.SignupForm;
import cz.osu.swi22025.model.json.UserToken;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final RabbitMQConfig rabbitMQConfig;
    private final RabbitAdmin rabbitAdmin;

    public ResponseEntity<String> signup(SignupForm userDTO) {
        ResponseEntity<String> ret;

        if (userRepository.existsByUsernameIgnoreCase(userDTO.getUsername())) {
            ret = new ResponseEntity<>("Username is already taken", HttpStatus.CONFLICT);
        } else {
            ChatUser user = new ChatUser();
            user.setUsername(userDTO.getUsername());
            user.setPassword(userDTO.getPassword());
            // Password encryption (BCrypt, salt, ...)
            user.addRoom(chatRoomRepository.findByChatNameIgnoreCase("Public"));

            ChatUser savedUser = userRepository.save(user);

            Queue userQueue = rabbitMQConfig.createUserQueue(savedUser.getUserId().toString());
            rabbitAdmin.declareQueue(userQueue);
            rabbitAdmin.declareBinding(rabbitMQConfig.bindUserQueue
                    (userQueue, rabbitMQConfig.chatroomExchange()));

            ret = new ResponseEntity<>("User registered successfully", HttpStatus.OK);
        }
        return ret;
    }

    public ResponseEntity<Object> authenticate(SignupForm userCredentials) {
        ResponseEntity<Object> ret;

        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(
                userCredentials.getUsername());
        if (user != null) {
            if (user.getPassword().equals(userCredentials.getPassword())) {
                UserToken userToken = new UserToken(user.getUserId(), user.getUsername());
                ret = new ResponseEntity<>(userToken, HttpStatus.OK);
            } else {
                ret = new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }
        } else {
            ret = new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        return ret;
    }
}
