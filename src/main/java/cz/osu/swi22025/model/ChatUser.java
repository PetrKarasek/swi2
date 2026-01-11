package cz.osu.swi22025.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, name = "user_id")
    private UUID userId;
    @Column(nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "timezone")
    private String timezone = "UTC";

    @Column(name = "current_room_id")
    private Integer currentRoomId;

    @ManyToMany
    @JoinTable(
            name = "chat_member",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "chat_id")
    )
    List<ChatRoom> joinedRooms = new ArrayList<>();

    @OneToMany(mappedBy = "chatUser")
    private List<Message> messages = new ArrayList<>();

    public void addRoom(ChatRoom chatRoom) {
        this.joinedRooms.add(chatRoom);
        chatRoom.getJoinedUsers().add(this);
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        message.setChatUser(this);
    }
}
