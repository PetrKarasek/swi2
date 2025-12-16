package cz.osu.swi22025.model;

import cz.osu.swi22025.model.db.Message;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Integer> {
    List<Message> findByChatRoom_ChatIdOrderBySendTimeAsc(Integer chatId);
}
