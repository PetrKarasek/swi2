package cz.osu.swi22025.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class DirectMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer messageId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private ChatUser sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private ChatUser receiver;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendTime;

    @Column(nullable = false)
    private Boolean isRead = false;
}
