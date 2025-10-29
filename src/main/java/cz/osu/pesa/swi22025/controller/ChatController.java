package cz.osu.pesa.swi22025.controller;

import cz.osu.pesa.swi22025.model.json.PayloadMessage;
import cz.osu.pesa.swi22025.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageService chatMessageService;

    @MessageMapping("/message") // url: /app/message
    public PayloadMessage receivePublicMessage(@Payload PayloadMessage message) {
        // todo: poslat zprávu do queue všech uživatelů
        chatMessageService.broadcastMessage(message);

        // todo: poslat zprávu přes websockety všem přihlášeným uživatelům, aby si ji vyzvedli

        // todo: práce s databází

        String destination = "/chatroom/" + message.getReceiverChatRoomId();
        messagingTemplate.convertAndSend(destination, message);
        return message;
    }

    @MessageMapping("/private-message") // url: /user/username/private
    public PayloadMessage receivePrivateMessage(@Payload PayloadMessage message) {
        // todo
        messagingTemplate.convertAndSendToUser(message.getReceiverName(), "/private", message);
        return message;
    }

}
