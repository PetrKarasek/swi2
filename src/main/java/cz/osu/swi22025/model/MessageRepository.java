package cz.osu.swi22025.model;

import cz.osu.swi22025.model.db.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Integer> {
}
