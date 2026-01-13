export interface PayloadMessage {
  senderName: string;
  receiverName?: string;
  receiverChatRoomId: string;
  content: string;
  date: string;
  senderAvatarUrl?: string;
  messageType?: string;
  fileUrl?: string;
  fileName?: string;
  isNotification?: boolean;
  notificationType?: string;
  
  id?: string; 
}

export interface User {
  userId: string;
  username: string;
}

export interface UserToken {
  userId: string;
  username: string;
}

export interface ChatRoom {
  chatId: number;
  chatName: string;
  joinedUsers?: User[];
  messages?: Message[];
}

export interface Message {
  messageId: number;
  chatRoom: ChatRoom;
  chatUser: User;
  content: string;
  sendTime: string;
}

export interface SignupForm {
  username: string;
  password: string;
}