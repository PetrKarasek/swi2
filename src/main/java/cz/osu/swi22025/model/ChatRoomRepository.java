package cz.osu.swi22025.model;

import cz.osu.swi22025.model.db.ChatRoom;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRoomRepository extends CrudRepository<ChatRoom, Integer> {
    List<ChatRoom> findByJoinedUsers_UserId(UUID userId);
    List<ChatRoom> findByJoinedUsers_Username(String username);
    boolean existsByChatNameIgnoreCase(String chatName);
    ChatRoom findByChatNameIgnoreCase(String chatName);
}
