package cz.osu.swi22025.model.json;

import lombok.Data;

@Data
public class PayloadMessage {
    private String senderName;
    private String receiverName;
    private String receiverChatRoomId;
    private String content;
    private String date;
}
