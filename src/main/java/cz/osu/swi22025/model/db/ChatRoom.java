package cz.osu.swi22025.model.db;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer chatId;

    @ManyToMany(mappedBy = "joinedRooms")
    private List<ChatUser> joinedUsers = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom")
    private List<Message> messages = new ArrayList<>();

    private String chatName;

    public void addMessage(Message message) {
        this.messages.add(message);
        message.setChatRoom(this);
    }
}
