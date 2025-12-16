package cz.osu.swi22025;

import cz.osu.swi22025.model.ChatRoomRepository;
import cz.osu.swi22025.model.db.ChatRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Swi22025Application implements CommandLineRunner {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public static void main(String[] args) {
        SpringApplication.run(Swi22025Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatName("Public");

        if (chatRoomRepository.existsByChatNameIgnoreCase("Public")) {
            System.out.println("Chat room 'Public' already exists");
        } else {
            chatRoomRepository.save(chatRoom);
            System.out.println("Chat room 'Public' created");
        }
    }

}
