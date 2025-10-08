package cz.osu.pesa.swi22025.model;

import cz.osu.pesa.swi22025.model.db.ChatUser;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRepository extends CrudRepository<ChatUser, UUID> {
    ChatUser findChatUserByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
