package cz.osu.swi22025.model;

import cz.osu.swi22025.model.ChatUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<ChatUser, UUID> {
    ChatUser findChatUserByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    
    // Add custom query to fix the 500 error
    @Query("SELECT u FROM ChatUser u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<ChatUser> findByUsernameIgnoreCase(@Param("username") String username);
}
