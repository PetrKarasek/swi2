package cz.osu.swi22025.model.json;

import lombok.Data;

@Data
public class PayloadMessage {
    private String senderName;
    private String receiverName;
    private String receiverChatRoomId;
    private String content;
    private String date;
    private String senderAvatarUrl;
    private String messageType = "TEXT";
    private String fileUrl;
    private String fileName;
    private Boolean isNotification = false;
    private String notificationType;
}
