package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.service.ChatMessageService;
import cz.osu.swi22025.service.NotificationService;
import cz.osu.swi22025.service.TimezoneService;
import cz.osu.swi22025.service.FileService;
import cz.osu.swi22025.service.AvatarService;
import cz.osu.swi22025.model.db.Message;
import cz.osu.swi22025.model.db.DirectMessage;
import cz.osu.swi22025.model.db.ChatUser;
import cz.osu.swi22025.model.db.ChatRoom;
import cz.osu.swi22025.model.MessageRepository;
import cz.osu.swi22025.model.DirectMessageRepository;
import cz.osu.swi22025.model.UserRepository;
import cz.osu.swi22025.model.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TimezoneService timezoneService;
    @Autowired
    private FileService fileService;
    @Autowired
    private AvatarService avatarService;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private DirectMessageRepository directMessageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MessageMapping("/message") // url: /app/message (WebSocket)
    public PayloadMessage receivePublicMessage(@Payload PayloadMessage message) {
        System.out.println("Received public message: " + message.getContent() + " from " + message.getSenderName());
        return handlePublicMessage(message);
    }

    // REST endpoint for sending message from non-WebSocket clients (e.g. desktop)
    @PostMapping("/api/message")
    @ResponseBody
    public PayloadMessage postPublicMessage(@RequestBody PayloadMessage message) {
        return handlePublicMessage(message);
    }

    // History of messages for a given chat room, loaded from the database.
    @GetMapping("/api/history")
    @ResponseBody
    public List<PayloadMessage> getHistory(@RequestParam String chatRoomId, @RequestParam(required = false) String username) {
        Integer id = Integer.valueOf(chatRoomId);
        ChatUser user = username != null ? userRepository.findChatUserByUsernameIgnoreCase(username) : null;
        String userTimezone = user != null ? user.getTimezone() : "UTC";
        
        return messageRepository.findByChatRoom_ChatIdOrderBySendTimeAsc(id)
                .stream()
                .map(m -> {
                    PayloadMessage p = new PayloadMessage();
                    p.setSenderName(m.getChatUser().getUsername());
                    p.setSenderAvatarUrl(m.getChatUser().getAvatarUrl());
                    p.setReceiverChatRoomId(String.valueOf(m.getChatRoom().getChatId()));
                    p.setContent(m.getContent());
                    p.setDate(timezoneService.convertToUserTimezone(m.getSendTime().toInstant(), userTimezone));
                    return p;
                })
                .collect(Collectors.toList());
    }

    private PayloadMessage handlePublicMessage(PayloadMessage message) {
        System.out.println("Handling public message: " + message.getContent());
        
        ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
        ChatRoom chatRoom = chatRoomRepository.findById(Integer.valueOf(message.getReceiverChatRoomId())).orElse(null);

        if (sender != null && chatRoom != null) {
            Message dbMessage = new Message();
            dbMessage.setChatUser(sender);
            dbMessage.setChatRoom(chatRoom);
            // Handle file messages
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                dbMessage.setContent("[FILE] " + message.getFileName() + " | " + message.getFileUrl());
            } else {
                dbMessage.setContent(message.getContent());
            }
            
            dbMessage.setSendTime(new Date());
            messageRepository.save(dbMessage);

            // Set sender avatar in message
            message.setSenderAvatarUrl(sender.getAvatarUrl());
            message.setDate(timezoneService.convertToUserTimezone(dbMessage.getSendTime().toInstant(), sender.getTimezone()));

            // Notify users in different rooms
            notificationService.notifyUsersInDifferentRooms(message, chatRoom.getChatId());
        }

        // Send message to RabbitMQ queue for all users (only if RabbitMQ is available)
        try {
            chatMessageService.broadcastMessage(message);
        } catch (Exception e) {
            // RabbitMQ not available, continue with WebSocket only
            System.out.println("RabbitMQ not available: " + e.getMessage());
        }

        // Send message via WebSocket to all subscribed users
        String destination = "/chatroom/" + message.getReceiverChatRoomId();
        System.out.println("Broadcasting to: " + destination);
        messagingTemplate.convertAndSend(destination, message);
        return message;
    }

    @MessageMapping("/private-message") // url: /user/username/private
    public PayloadMessage receivePrivateMessage(@Payload PayloadMessage message) {
        System.out.println("Received private message: " + message.getContent() + " from " + message.getSenderName() + " to " + message.getReceiverName());
        return handlePrivateMessage(message);
    }

    // REST endpoint for sending private message from non-WebSocket clients
    @PostMapping("/api/private-message")
    @ResponseBody
    public PayloadMessage postPrivateMessage(@RequestBody PayloadMessage message) {
        return handlePrivateMessage(message);
    }

    private PayloadMessage handlePrivateMessage(PayloadMessage message) {
        // Save private message to database
        ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
        ChatUser receiver = userRepository.findChatUserByUsernameIgnoreCase(message.getReceiverName());

        if (sender != null && receiver != null) {
            // Save to database
            DirectMessage directMessage = new DirectMessage();
            directMessage.setSender(sender);
            directMessage.setReceiver(receiver);
            
            // Handle file messages
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                directMessage.setContent("[FILE] " + message.getFileName() + " | " + message.getFileUrl());
            } else {
                directMessage.setContent(message.getContent());
            }
            
            directMessage.setSendTime(new Date());
            directMessage.setIsRead(false);
            directMessageRepository.save(directMessage);

            // Set sender avatar and timezone-aware date
            message.setSenderAvatarUrl(sender.getAvatarUrl());
            message.setDate(timezoneService.convertToUserTimezone(directMessage.getSendTime().toInstant(), sender.getTimezone()));

            // Send message via WebSocket to specific user
            messagingTemplate.convertAndSendToUser(message.getReceiverName(), "/private", message);

            // Notify receiver of new private message
            notificationService.notifyUserOfPrivateMessage(message);
        }
        return message;
    }

    // Get direct message history between two users
    @GetMapping("/api/direct-history")
    @ResponseBody
    public List<PayloadMessage> getDirectHistory(@RequestParam String user1, @RequestParam String user2) {
        ChatUser currentUser1 = userRepository.findChatUserByUsernameIgnoreCase(user1);
        ChatUser currentUser2 = userRepository.findChatUserByUsernameIgnoreCase(user2);
        String userTimezone = currentUser1 != null ? currentUser1.getTimezone() : "UTC";
        
        if (currentUser1 != null && currentUser2 != null) {
            return directMessageRepository.findConversationBetweenUsers(currentUser1, currentUser2)
                    .stream()
                    .map(dm -> {
                        PayloadMessage p = new PayloadMessage();
                        p.setSenderName(dm.getSender().getUsername());
                        p.setSenderAvatarUrl(dm.getSender().getAvatarUrl());
                        p.setReceiverName(dm.getReceiver().getUsername());
                        p.setContent(dm.getContent());
                        p.setDate(timezoneService.convertToUserTimezone(dm.getSendTime().toInstant(), userTimezone));
                        return p;
                    })
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // Get unread messages for a user
    @GetMapping("/api/unread-messages")
    @ResponseBody
    public List<PayloadMessage> getUnreadMessages(@RequestParam String username) {
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        String userTimezone = user != null ? user.getTimezone() : "UTC";
        
        if (user != null) {
            return directMessageRepository.findUnreadMessages(user)
                    .stream()
                    .map(dm -> {
                        PayloadMessage p = new PayloadMessage();
                        p.setSenderName(dm.getSender().getUsername());
                        p.setSenderAvatarUrl(dm.getSender().getAvatarUrl());
                        p.setReceiverName(dm.getReceiver().getUsername());
                        p.setContent(dm.getContent());
                        p.setDate(timezoneService.convertToUserTimezone(dm.getSendTime().toInstant(), userTimezone));
                        return p;
                    })
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // Mark messages as read
    @PostMapping("/api/mark-messages-read")
    @ResponseBody
    public void markMessagesAsRead(@RequestParam String username) {
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        
        if (user != null) {
            List<DirectMessage> unreadMessages = directMessageRepository.findUnreadMessages(user);
            unreadMessages.forEach(dm -> dm.setIsRead(true));
            directMessageRepository.saveAll(unreadMessages);
        }
    }

    // Update user's current room
    @PostMapping("/api/update-current-room")
    @ResponseBody
    public void updateCurrentRoom(@RequestParam String username, @RequestParam Integer roomId) {
        notificationService.updateUserCurrentRoom(username, roomId);
    }

    // Upload avatar
    @PostMapping("/api/upload-avatar")
    @ResponseBody
    public String uploadAvatar(@RequestParam String username, @RequestParam MultipartFile file) {
        try {
            ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
            if (user != null) {
                String fileUrl = fileService.storeFile(file);
                user.setAvatarUrl(fileUrl);
                userRepository.save(user);
                return fileUrl;
            }
        } catch (Exception e) {
            return "Error uploading avatar: " + e.getMessage();
        }
        return null;
    }

    // Upload file in chat
    @PostMapping("/api/upload-file")
    @ResponseBody
    public PayloadMessage uploadFile(@RequestParam String username, 
                                    @RequestParam String receiverChatRoomId,
                                    @RequestParam(required = false) String receiverName,
                                    @RequestParam MultipartFile file) {
        try {
            ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(username);
            if (sender == null) return null;

            String fileUrl = fileService.storeFile(file);
            boolean isImage = fileService.isImageFile(file.getOriginalFilename());
            
            PayloadMessage message = new PayloadMessage();
            message.setSenderName(username);
            message.setSenderAvatarUrl(sender.getAvatarUrl());
            message.setReceiverChatRoomId(receiverChatRoomId);
            message.setReceiverName(receiverName);
            message.setContent(file.getOriginalFilename());
            message.setFileUrl(fileUrl);
            message.setFileName(file.getOriginalFilename());
            message.setMessageType(isImage ? "IMAGE" : "FILE");
            message.setDate(timezoneService.getCurrentTimeInTimezone(sender.getTimezone()));

            if (receiverName != null && !receiverName.isEmpty()) {
                // Private file message
                return handlePrivateMessage(message);
            } else {
                // Room file message
                return handlePublicMessage(message);
            }
        } catch (Exception e) {
            return null;
        }
    }

    // Test endpoint to check avatar persistence
    @GetMapping("/api/test-avatar/{username}")
    @ResponseBody
    public String testAvatar(@PathVariable String username) {
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        if (user != null) {
            return "User: " + username + ", Avatar: " + user.getAvatarUrl();
        }
        return "User not found: " + username;
    }

    // Get user info including avatar
    @GetMapping("/api/user/{username}")
    @ResponseBody
    public Object getUserInfo(@PathVariable String username) {
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        if (user == null) {
            return null;
        }
        // Return a simple map to avoid circular reference issues
        return java.util.Map.of(
            "userId", user.getUserId().toString(),
            "username", user.getUsername(),
            "avatarUrl", user.getAvatarUrl(),
            "timezone", user.getTimezone()
        );
    }

    // Get available avatars
    @GetMapping("/api/avatars")
    @ResponseBody
    public List<String> getAvailableAvatars() {
        return avatarService.getAllAvatars();
    }

    // Select avatar from predefined list
    @PostMapping("/api/select-avatar")
    @ResponseBody
    public void selectAvatar(@RequestParam String username, @RequestParam int avatarIndex) {
        System.out.println("Selecting avatar for user: " + username + ", index: " + avatarIndex);
        ChatUser user = userRepository.findChatUserByUsernameIgnoreCase(username);
        if (user != null) {
            String avatarUrl = avatarService.getAvatarUrl(avatarIndex);
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            System.out.println("Avatar saved: " + avatarUrl + " for user: " + username);
        } else {
            System.out.println("User not found: " + username);
        }
    }

}
