package cz.osu.swi22025.service;

import cz.osu.swi22025.model.ChatUser;
import cz.osu.swi22025.model.ChatRoom;
import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public void notifyUsersInDifferentRooms(PayloadMessage message, Integer chatRoomId) {
        ChatRoom messageRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (messageRoom == null) return;

        List<ChatUser> roomMembers = messageRoom.getJoinedUsers();
        
        List<ChatUser> usersToNotify = roomMembers.stream()
                .filter(user -> user.getCurrentRoomId() == null || !user.getCurrentRoomId().equals(chatRoomId))
                .collect(Collectors.toList());

        for (ChatUser user : usersToNotify) {
            PayloadMessage notification = createNotification(message, "NEW_MESSAGE", chatRoomId);
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/notifications", notification);
        }
    }

    public void notifyUserOfPrivateMessage(PayloadMessage message) {
        ChatUser receiver = userRepository.findByUsernameIgnoreCase(message.getReceiverName())
            .orElse(null);
        if (receiver != null) {
            PayloadMessage notification = createNotification(message, "PRIVATE_MESSAGE", null);
            messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/notifications", notification);
        }
    }

    private PayloadMessage createNotification(PayloadMessage originalMessage, String notificationType, Integer chatRoomId) {
        PayloadMessage notification = new PayloadMessage();
        notification.setSenderName(originalMessage.getSenderName());
        notification.setContent(originalMessage.getContent());
        notification.setDate(originalMessage.getDate());
        notification.setIsNotification(true);
        notification.setNotificationType(notificationType);
        notification.setReceiverChatRoomId(chatRoomId != null ? chatRoomId.toString() : null);
        notification.setSenderAvatarUrl(originalMessage.getSenderAvatarUrl());
        return notification;
    }

    public void updateUserCurrentRoom(String username, Integer roomId) {
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        if (user != null) {
            user.setCurrentRoomId(roomId);
            userRepository.save(user);
        }
    }
}
