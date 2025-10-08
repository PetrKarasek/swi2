package cz.osu.pesa.swi22025.model.db;

import jakarta.persistence.*;
import lombok.Data;

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
}
