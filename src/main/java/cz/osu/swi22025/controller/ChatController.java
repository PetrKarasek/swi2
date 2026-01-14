package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import cz.osu.swi22025.service.*;
import cz.osu.swi22025.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private NotificationService notificationService;
    @Autowired private TimezoneService timezoneService;
    @Autowired private FileService fileService;
    @Autowired private AvatarService avatarService;
    @Autowired private MessageRepository messageRepository;
    @Autowired private DirectMessageRepository directMessageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;

    @MessageMapping("/message")
    @SendTo("/chatroom/1")
    public PayloadMessage receivePublicMessage(@Payload PayloadMessage message) {

        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(UUID.randomUUID().toString());
        }

        return savePublicMessageToDB(message);
    }

    @PostMapping("/api/message")
    @ResponseBody
    public PayloadMessage postPublicMessage(@RequestBody PayloadMessage message) {

        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(UUID.randomUUID().toString());
        }

        PayloadMessage saved = savePublicMessageToDB(message);
        messagingTemplate.convertAndSend("/chatroom/" + saved.getReceiverChatRoomId(), saved);
        return saved;
    }

    private PayloadMessage savePublicMessageToDB(PayloadMessage message) {
        try {
            ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
            Integer roomId = 1;
            try { roomId = Integer.valueOf(message.getReceiverChatRoomId()); } catch (Exception e) {}

            ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElse(null);

            if (sender != null && chatRoom != null) {
                Message dbMessage = new Message();
                dbMessage.setChatUser(sender);
                dbMessage.setChatRoom(chatRoom);

                if (message.getFileUrl() != null) {
                    dbMessage.setContent("[FILE] " + message.getFileName() + " | " + message.getFileUrl());
                } else {
                    dbMessage.setContent(message.getContent());
                }
                dbMessage.setSendTime(new Date());
                messageRepository.save(dbMessage);

                // 🔑 sjednocení: do payloadu posílej vždy normalizovanou RELATIVNÍ cestu (/avatars/..)
                message.setSenderAvatarUrl(avatarService.normalizeAvatarUrl(sender.getAvatarUrl()));
                message.setDate(timezoneService.convertToUserTimezone(dbMessage.getSendTime().toInstant(), sender.getTimezone()));
                message.setReceiverChatRoomId(String.valueOf(roomId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @PostMapping("/api/upload-file")
    @ResponseBody
    public PayloadMessage uploadFile(@RequestParam String username,
                                     @RequestParam String receiverChatRoomId,
                                     @RequestParam(required = false) String receiverName,
                                     @RequestParam(required = false) String id,
                                     @RequestParam MultipartFile file) {
        try {
            ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(username);
            if (sender == null) return null;

            String fileUrl = fileService.storeFile(file);
            boolean isImage = fileService.isImageFile(file.getOriginalFilename());

            PayloadMessage message = new PayloadMessage();
            if (id != null && !id.isEmpty()) {
                message.setId(id);
            } else {
                message.setId(UUID.randomUUID().toString());
            }

            message.setSenderName(username);
            message.setSenderAvatarUrl(avatarService.normalizeAvatarUrl(sender.getAvatarUrl()));
            message.setReceiverChatRoomId(receiverChatRoomId);
            message.setReceiverName(receiverName);
            message.setContent(file.getOriginalFilename());
            message.setFileUrl(fileUrl);
            message.setFileName(file.getOriginalFilename());
            message.setMessageType(isImage ? "IMAGE" : "FILE");
            message.setDate(timezoneService.getCurrentTimeInTimezone(sender.getTimezone()));

            if (receiverName != null && !receiverName.isEmpty()) {
                return handlePrivateMessage(message);
            } else {
                PayloadMessage saved = savePublicMessageToDB(message);
                messagingTemplate.convertAndSend("/chatroom/" + receiverChatRoomId, saved);
                return saved;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @MessageMapping("/private-message")
    public PayloadMessage receivePrivateMessage(@Payload PayloadMessage message) {
        return handlePrivateMessage(message);
    }

    @PostMapping("/api/private-message")
    @ResponseBody
    public PayloadMessage postPrivateMessage(@RequestBody PayloadMessage message) {
        return handlePrivateMessage(message);
    }

    private PayloadMessage handlePrivateMessage(PayloadMessage message) {

        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(UUID.randomUUID().toString());
        }

        ChatUser sender = userRepository.findChatUserByUsernameIgnoreCase(message.getSenderName());
        ChatUser receiver = userRepository.findChatUserByUsernameIgnoreCase(message.getReceiverName());

        if (sender != null && receiver != null) {
            DirectMessage dm = new DirectMessage();
            dm.setSender(sender);
            dm.setReceiver(receiver);
            if (message.getFileUrl() != null) {
                dm.setContent("[FILE] " + message.getFileName() + " | " + message.getFileUrl());
            } else {
                dm.setContent(message.getContent());
            }
            dm.setSendTime(new Date());
            dm.setIsRead(false);
            directMessageRepository.save(dm);

            message.setSenderAvatarUrl(avatarService.normalizeAvatarUrl(sender.getAvatarUrl()));
            message.setDate(timezoneService.convertToUserTimezone(dm.getSendTime().toInstant(), sender.getTimezone()));

            messagingTemplate.convertAndSendToUser(message.getReceiverName(), "/private", message);
            messagingTemplate.convertAndSendToUser(message.getSenderName(), "/private", message);

            notificationService.notifyUserOfPrivateMessage(message);
        }
        return message;
    }

    @GetMapping("/api/history")
    @ResponseBody
    public List<PayloadMessage> getHistory(@RequestParam String chatRoomId, @RequestParam(required = false) String username) {
        Integer id = Integer.valueOf(chatRoomId);
        ChatUser user = username != null ? userRepository.findChatUserByUsernameIgnoreCase(username) : null;
        String userTimezone = user != null ? user.getTimezone() : "UTC";

        return messageRepository.findByChatRoom_ChatIdOrderBySendTimeAsc(id)
                .stream()
                .map(m -> mapToPayload(m, userTimezone))
                .collect(Collectors.toList());
    }

    private PayloadMessage mapToPayload(Message m, String userTimezone) {
        PayloadMessage p = new PayloadMessage();
        p.setId(UUID.randomUUID().toString());
        p.setSenderName(m.getChatUser().getUsername());
        p.setSenderAvatarUrl(avatarService.normalizeAvatarUrl(m.getChatUser().getAvatarUrl()));
        p.setReceiverChatRoomId(String.valueOf(m.getChatRoom().getChatId()));
        p.setContent(m.getContent());
        if (m.getContent() != null && m.getContent().startsWith("[FILE]")) {
            try {
                String[] parts = m.getContent().replace("[FILE]", "").split("\\|");
                if (parts.length >= 2) {
                    p.setFileName(parts[0].trim());
                    p.setFileUrl(parts[1].trim());
                }
            } catch (Exception e) {}
        }
        p.setDate(timezoneService.convertToUserTimezone(m.getSendTime().toInstant(), userTimezone));
        return p;
    }

    @GetMapping("/api/direct-history")
    @ResponseBody
    public List<PayloadMessage> getDirectHistory(@RequestParam String user1, @RequestParam String user2) {
        ChatUser u1 = userRepository.findChatUserByUsernameIgnoreCase(user1);
        ChatUser u2 = userRepository.findChatUserByUsernameIgnoreCase(user2);
        String tz = u1 != null ? u1.getTimezone() : "UTC";

        if (u1 != null && u2 != null) {
            return directMessageRepository.findConversationBetweenUsers(u1, u2).stream().map(dm -> {
                PayloadMessage p = new PayloadMessage();
                p.setId(UUID.randomUUID().toString());
                p.setSenderName(dm.getSender().getUsername());
                p.setSenderAvatarUrl(avatarService.normalizeAvatarUrl(dm.getSender().getAvatarUrl()));
                p.setReceiverName(dm.getReceiver().getUsername());
                p.setContent(dm.getContent());
                if (dm.getContent() != null && dm.getContent().startsWith("[FILE]")) {
                    try {
                        String[] parts = dm.getContent().replace("[FILE]", "").split("\\|");
                        if (parts.length >= 2) {
                            p.setFileName(parts[0].trim());
                            p.setFileUrl(parts[1].trim());
                        }
                    } catch (Exception e) {}
                }
                p.setDate(timezoneService.convertToUserTimezone(dm.getSendTime().toInstant(), tz));
                return p;
            }).collect(Collectors.toList());
        }
        return List.of();
    }

    @GetMapping("/api/unread-messages")
    @ResponseBody
    public List<PayloadMessage> getUnreadMessages(@RequestParam String username) {
        ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        String tz = user != null ? user.getTimezone() : "UTC";
        if (user != null) {
            return directMessageRepository.findUnreadMessages(user).stream().map(dm -> {
                PayloadMessage p = new PayloadMessage();
                p.setId(UUID.randomUUID().toString());
                p.setSenderName(dm.getSender().getUsername());
                p.setContent(dm.getContent());
                p.setDate(timezoneService.convertToUserTimezone(dm.getSendTime().toInstant(), tz));
                return p;
            }).collect(Collectors.toList());
        }
        return List.of();
    }

    @PostMapping("/api/mark-messages-read")
    @ResponseBody
    public void markMessagesAsRead(@RequestParam String username) {
        ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user != null) {
            List<DirectMessage> unread = directMessageRepository.findUnreadMessages(user);
            unread.forEach(dm -> dm.setIsRead(true));
            directMessageRepository.saveAll(unread);
        }
    }

    @PostMapping("/api/upload-avatar")
    @ResponseBody
    public String uploadAvatar(@RequestParam String username, @RequestParam MultipartFile file) {
        try {
            ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
            if (user != null) {
                String url = fileService.storeFile(file);
                user.setAvatarUrl(url);
                userRepository.save(user);
                return url;
            }
        } catch (Exception e) {}
        return null;
    }

    // Avatars for both web and desktop: return RELATIVE urls ("/avatars/...")
    @GetMapping("/api/avatars")
    @ResponseBody
    public List<String> getAvatars() {
        // vrací RELATIVNÍ URL (/avatars/..). Web/desktop si to doplní o host.
        return avatarService.getAvailableAvatars();
    }

    @PostMapping("/api/select-avatar")
    @ResponseBody
    public void selectAvatar(@RequestParam String username, @RequestParam int avatarIndex) {
        ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user != null) {
            user.setAvatarUrl(avatarService.avatarByIndex(avatarIndex));
            userRepository.save(user);
        }
    }

    @GetMapping("/api/user/{username}")
    @ResponseBody
    public Object getUserInfo(@PathVariable String username) {
        ChatUser user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return java.util.Map.of(
                    "username", username,
                    "avatarUrl", avatarService.getDefaultAvatar()
            );
        }
        return java.util.Map.of(
                "userId", user.getUserId().toString(),
                "username", user.getUsername(),
                "avatarUrl", avatarService.normalizeAvatarUrl(user.getAvatarUrl()),
                "timezone", user.getTimezone()
        );
    }
}
