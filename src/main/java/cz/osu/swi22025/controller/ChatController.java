package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.service.ChatMessageService;
import cz.osu.swi22025.model.db.Message;
import cz.osu.swi22025.model.db.ChatUser;
import cz.osu.swi22025.model.db.ChatRoom;
import cz.osu.swi22025.model.MessageRepository;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Date;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MessageMapping("/message") // url: /app/message
    public PayloadMessage receivePublicMessage(@Payload PayloadMessage message) {
        // Save message to database
        ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
        ChatRoom chatRoom = chatRoomRepository.findById(Integer.valueOf(message.getReceiverChatRoomId())).orElse(null);
        
        if (sender != null && chatRoom != null) {
            Message dbMessage = new Message();
            dbMessage.setChatUser(sender);
            dbMessage.setChatRoom(chatRoom);
            dbMessage.setContent(message.getContent());
            dbMessage.setSendTime(new Date());
            messageRepository.save(dbMessage);
        }

        // Send message to RabbitMQ queue for all users
        chatMessageService.broadcastMessage(message);

        // Send message via WebSocket to all subscribed users
        String destination = "/chatroom/" + message.getReceiverChatRoomId();
        messagingTemplate.convertAndSend(destination, message);
        return message;
    }

    @MessageMapping("/private-message") // url: /user/username/private
    public PayloadMessage receivePrivateMessage(@Payload PayloadMessage message) {
        // Save private message to database
        ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
        ChatUser receiver = userRepository.findChatUserByUsernameIgnoreCase(message.getReceiverName());
        
        if (sender != null && receiver != null) {
            // For private messages, we could create a separate chat room or handle differently
            // For now, send directly via WebSocket
            messagingTemplate.convertAndSendToUser(message.getReceiverName(), "/private", message);
        }
        return message;
    }

}
