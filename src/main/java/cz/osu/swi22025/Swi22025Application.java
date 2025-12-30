package cz.osu.swi22025;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import cz.osu.swi22025.service.UserService;
import cz.osu.swi22025.model.ChatRoomRepository;
import cz.osu.swi22025.model.db.ChatRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class Swi22025Application implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public static void main(String[] args) {
        SpringApplication.run(Swi22025Application.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:5173")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false);
            }
        };
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
