package cz.osu.swi22025.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PayloadMessage {
    private String id;

    private String senderName;
    private String receiverName;
    
    @JsonProperty("receiverChatRoomId")
    private String receiverChatRoomId;
    
    private String content;
    private String date;
    private String senderAvatarUrl;
    private String messageType; 
    private String fileUrl;
    private String fileName;
    
    private boolean isNotification;
    private String notificationType;

    public PayloadMessage() {
    }

    public PayloadMessage(String senderName, String receiverName, String receiverChatRoomId, String content, String date, String senderAvatarUrl, String messageType, String fileUrl, String fileName, boolean isNotification, String notificationType) {
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.receiverChatRoomId = receiverChatRoomId;
        this.content = content;
        this.date = date;
        this.senderAvatarUrl = senderAvatarUrl;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.isNotification = isNotification;
        this.notificationType = notificationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIsNotification(boolean isNotification) {
        this.isNotification = isNotification;
    }

    public boolean getIsNotification() {
        return isNotification;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverChatRoomId() {
        return receiverChatRoomId;
    }

    public void setReceiverChatRoomId(String receiverChatRoomId) {
        this.receiverChatRoomId = receiverChatRoomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    @Override
    public String toString() {
        return "PayloadMessage{" +
                "id='" + id + '\'' + 
                ", senderName='" + senderName + '\'' +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}