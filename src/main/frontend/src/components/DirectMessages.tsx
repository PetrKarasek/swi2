import { 
  Box, 
  Dialog, 
  DialogTitle, 
  DialogContent,
  DialogActions,
  Button,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Paper,
  List,
  ListItem,
  TextField
} from '@mui/material';
import React, { useEffect, useState, useRef } from "react";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, UserToken } from "../types";

const DIRECT_HISTORY_URL = "http://localhost:8081/api/direct-history";
const UNREAD_MESSAGES_URL = "http://localhost:8081/api/unread-messages";
const MARK_READ_URL = "http://localhost:8081/api/mark-messages-read";

interface DirectMessagesProps {
  user: UserToken | null;
  stompClient: Client | null;
  allUsers: string[];
}

const DirectMessages = ({ user, stompClient, allUsers }: DirectMessagesProps) => {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<PayloadMessage[]>([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [unreadCount, setUnreadCount] = useState(0);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isUserSelectOpen, setIsUserSelectOpen] = useState(false);
  const [users, setUsers] = useState<string[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (user && stompClient && stompClient.connected) {
      // Subscribe to private messages for this user
      const subscription = stompClient.subscribe(`/user/${user.username}/private`, onPrivateMessageReceived);
      
      return () => {
        subscription.unsubscribe();
      };
    }
  }, [user, stompClient]);

  useEffect(() => {
    if (user) {
      loadUnreadMessages();
    }
  }, [user]);

  useEffect(() => {
    setUsers(allUsers);
    console.log("DirectMessages - allUsers updated:", allUsers);
  }, [allUsers]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const onPrivateMessageReceived = (payload: any) => {
    const payloadData: PayloadMessage = JSON.parse(payload.body);
    console.log("Private message received from: " + payloadData.senderName);
    console.log("Full private message data:", payloadData);
    
    if (selectedUser === payloadData.senderName || selectedUser === payloadData.receiverName) {
      setMessages((prev) => [...prev, payloadData]);
    } else {
      loadUnreadMessages();
    }
  };

  const loadUnreadMessages = async () => {
    if (!user) return;
    
    try {
      const params = new URLSearchParams([["username", user.username]]);
      const result = await axios.get<PayloadMessage[]>(UNREAD_MESSAGES_URL, { params });
      setUnreadCount(result.data.length);
    } catch (e) {
      console.log("Error loading unread messages:", e);
    }
  };

  const loadDirectHistory = async (targetUser: string) => {
    if (!user || !targetUser) return;
    
    try {
      const params = new URLSearchParams([
        ["user1", user.username],
        ["user2", targetUser]
      ]);
      const result = await axios.get<PayloadMessage[]>(DIRECT_HISTORY_URL, { params });
      setMessages(result.data);
    } catch (e) {
      console.log("Error loading direct history:", e);
    }
  };

  const openDirectMessage = (targetUser: string) => {
    setSelectedUser(targetUser);
    loadDirectHistory(targetUser);
    setIsDialogOpen(true);
    markMessagesAsRead();
  };

  const markMessagesAsRead = async () => {
    if (!user) return;
    
    try {
      const params = new URLSearchParams([["username", user.username]]);
      await axios.post(MARK_READ_URL, null, { params });
      setUnreadCount(0);
    } catch (e) {
      console.log("Error marking messages as read:", e);
    }
  };

  const sendDirectMessage = () => {
    if (!message.trim() || !selectedUser || !user) return;
    if (!stompClient || !stompClient.connected) {
      console.error("WebSocket not connected");
      return;
    }

    const payloadMessage: PayloadMessage = {
      senderName: user.username,
      receiverName: selectedUser,
      receiverChatRoomId: "",
      content: message.trim(),
      date: new Date().toISOString(),
    };

    stompClient.publish({
      destination: "/app/private-message",
      body: JSON.stringify(payloadMessage),
    });

    setMessage("");
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendDirectMessage();
    }
  };

  const handleCloseDialog = () => {
    setIsDialogOpen(false);
    setSelectedUser("");
    setMessages([]);
  };

  return (
    <Box sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" sx={{ mb: 2 }}>
        Direct Messages
      </Typography>
      
      <Box sx={{ mb: 2 }}>
        {unreadCount > 0 && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Typography variant="body2" sx={{ color: 'red', fontWeight: 'bold' }}>
              {unreadCount} unread message{unreadCount > 1 ? 's' : ''}
            </Typography>
            <Button 
              variant="outlined" 
              size="small"
              onClick={() => {
                // Open a dialog to show unread messages or select a user
                setIsUserSelectOpen(true);
              }}
            >
              View Messages
            </Button>
          </Box>
        )}
      </Box>

      <Button 
        variant="contained" 
        onClick={() => setIsUserSelectOpen(true)}
        sx={{ mb: 2 }}
      >
        Choose User to Message
      </Button>

      {/* User Selection Dialog */}
      <Dialog open={isUserSelectOpen} onClose={() => setIsUserSelectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Select User</DialogTitle>
        <DialogContent>
          <List>
            {users
              .filter(u => u !== user?.username)
              .map((username) => (
                <ListItem 
                  key={username} 
                  onClick={() => {
                    openDirectMessage(username);
                    setIsUserSelectOpen(false);
                  }}
                  sx={{ cursor: 'pointer' }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                    <Typography>{username}</Typography>
                    {unreadCount > 0 && <span style={{ color: 'red' }}>●</span>}
                  </Box>
                </ListItem>
              ))}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsUserSelectOpen(false)}>Cancel</Button>
        </DialogActions>
      </Dialog>

      <Dialog 
        open={isDialogOpen} 
        onClose={handleCloseDialog} 
        maxWidth="md" 
        fullWidth
        PaperProps={{ 
          sx: { 
            bgcolor: 'background.paper',
            color: 'text.primary'
          } 
        }}
      >
        <DialogTitle>
          Direct Message with {selectedUser}
        </DialogTitle>
        <DialogContent>
          <Paper sx={{ p: 2, mb: 2, maxHeight: 400, overflow: 'auto' }}>
            <List>
              {messages.map((msg: PayloadMessage, index: number) => (
                <ListItem key={index} sx={{ p: 0, mb: 1 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: msg.senderName === user?.username ? 'flex-end' : 'flex-start',
                      width: '100%'
                    }}
                  >
                    <Box
                      sx={{
                        maxWidth: '70%',
                        p: 2,
                        borderRadius: 2,
                        backgroundColor: msg.senderName === user?.username ? '#1976d2' : '#e0e0e0',
                        color: msg.senderName === user?.username ? 'white' : 'black'
                      }}
                    >
                      <Typography variant="caption" sx={{ display: 'block', mb: 0.5 }}>
                        {msg.senderName}
                      </Typography>
                      <Typography variant="body1">
                        {msg.content}
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7 }}>
                        {new Date(msg.date).toLocaleTimeString()}
                      </Typography>
                    </Box>
                  </Box>
                </ListItem>
              ))}
              <div ref={messagesEndRef} />
            </List>
          </Paper>
          <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Type a message..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              onKeyPress={handleKeyPress}
            />
            <Button 
              variant="contained" 
              onClick={sendDirectMessage}
              disabled={!message.trim()}
            >
              Send
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DirectMessages;
