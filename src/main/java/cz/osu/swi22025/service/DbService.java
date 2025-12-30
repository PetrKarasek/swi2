package cz.osu.swi22025.service;

import cz.osu.swi22025.model.ChatRoomRepository;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.db.ChatRoom;
import cz.osu.swi22025.model.db.ChatUser;
import cz.osu.swi22025.model.db.Message;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class DbService {
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ResponseEntity<List<String>> getUsers() {
        List<ChatUser> chatUsers = (List<ChatUser>) userRepository.findAll();
        List<String> usernames = chatUsers.stream()
                .map(ChatUser::getUsername)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(usernames);
    }

    public ResponseEntity<List<ChatRoom>> getChatRooms(String username) {
        List<ChatRoom> chatRooms = chatRoomRepository
                .findByJoinedUsers_Username(username);
        chatRooms.forEach(chatRoom -> chatRoom.setJoinedUsers(null));
        for (ChatRoom chatRoom : chatRooms) {
            for (Message message : chatRoom.getMessages()) {
                message.setChatRoom(null);
                message.getChatUser().setMessages(null);
                message.getChatUser().setJoinedRooms(null);
            }
        }
        return ResponseEntity.ok(chatRooms);
    }

}
