import { 
  Box, Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, 
  List, ListItem, TextField, Paper, IconButton, InputAdornment
} from '@mui/material';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import ImageIcon from '@mui/icons-material/Image';
import DescriptionIcon from '@mui/icons-material/Description';
import React, { useEffect, useState, useRef } from "react";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, UserToken } from "../types";
import Avatar from "./Avatar";
import DMConversationsList from "./DMConversationsList";

const DIRECT_HISTORY_URL = "http://localhost:8081/api/direct-history";
const MARK_READ_URL = "http://localhost:8081/api/mark-messages-read";
const FILE_UPLOAD_URL = "http://localhost:8081/api/upload-file";

const generateId = () => {
    return Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
};

interface DirectMessagesProps {
  user: UserToken | null;
  stompClient: Client | null;
  allUsers: string[];
  conversations: {[username: string]: PayloadMessage[]};
  setConversations: React.Dispatch<React.SetStateAction<{ [username: string]: PayloadMessage[] }>>;
  unreadDMs?: number;
  onUnreadDMsChange?: (count: number | ((prev: number) => number)) => void;
  unreadPerUser?: {[key: string]: number};
  clearUnreadForUser?: (username: string) => void;
}

const extractFileInfo = (msg: PayloadMessage) => {
  if (msg.fileUrl) return { fileUrl: msg.fileUrl, fileName: msg.fileName || 'download', isImage: msg.fileName?.match(/\.(jpg|jpeg|png|gif|webp)$/i) };
  if (msg.content && typeof msg.content === 'string') {
    const match = msg.content.match(/^\s*\[FILE\]\s*(.*?)\s*\|\s*(.*)$/);
    if (match && match.length >= 3) return { fileUrl: match[2].trim(), fileName: match[1].trim(), isImage: match[1].match(/\.(jpg|jpeg|png|gif|webp)$/i) };
  }
  return null;
};

const safeFormatDate = (dateString: any) => {
  if (!dateString) return "";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "";
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch (e) { return ""; }
};

const DirectMessages = ({ 
    user, stompClient, allUsers, unreadDMs, onUnreadDMsChange, conversations = {}, setConversations,
    unreadPerUser, clearUnreadForUser 
}: DirectMessagesProps) => {
  
  const [message, setMessage] = useState("");
  const [activeMessages, setActiveMessages] = useState<PayloadMessage[]>([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isUserSelectOpen, setIsUserSelectOpen] = useState(false);
  const [userAvatars, setUserAvatars] = useState<{[key: string]: string}>({});
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    try { if (selectedUser && conversations[selectedUser]) setActiveMessages(conversations[selectedUser]); } catch (error) {}
  }, [conversations, selectedUser]);
  
  useEffect(() => { if (isDialogOpen) setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: "smooth" }), 100); }, [activeMessages, isDialogOpen]);

  useEffect(() => {
    const loadUserAvatars = async () => {
      const avatars: {[key: string]: string} = {};
      const usersToLoad = allUsers || [];
      for (const username of usersToLoad) {
        if (username === user?.username) continue;
        try { const response = await axios.get(`http://localhost:8081/api/user/${username}`); avatars[username] = response.data?.avatarUrl || "http://localhost:8081/avatars/cat.png"; } 
        catch (e) { avatars[username] = "http://localhost:8081/avatars/cat.png"; }
      }
      setUserAvatars(avatars);
    };
    if (allUsers?.length > 0) loadUserAvatars();
  }, [allUsers, user?.username]);

  const loadDirectHistory = async (targetUser: string) => {
    if (!user || !targetUser) return;
    try {
      const params = new URLSearchParams([["user1", user.username], ["user2", targetUser]]);
      const result = await axios.get<PayloadMessage[]>(DIRECT_HISTORY_URL, { params });
      const data = Array.isArray(result.data) ? result.data : [];
      setActiveMessages(data);
      setConversations(prev => ({ ...prev, [targetUser]: data }));
    } catch (e) { setActiveMessages([]); }
  };

  const markMessagesAsRead = async () => {
    if (!user) return;
    try {
      const params = new URLSearchParams([["username", user.username]]);
      await axios.post(MARK_READ_URL, null, { params });
    } catch (e) { }
  };

  const openDirectMessage = (targetUser: string) => {
    if (!targetUser || !user) return;
    setSelectedUser(targetUser);
    setActiveMessages([]); 

    try {
      if (conversations[targetUser]) setActiveMessages(conversations[targetUser]);
      else loadDirectHistory(targetUser);
    } catch (error) { loadDirectHistory(targetUser); }
    
    setIsDialogOpen(true);
    markMessagesAsRead();
    
    if (clearUnreadForUser) {
        clearUnreadForUser(targetUser);
    }
  };

  const handleFileUpload = async (file: File, id: string) => {
      const formData = new FormData(); 
      formData.append('file', file); 
      formData.append('username', user!.username); 
      formData.append('receiverName', selectedUser); 
      formData.append('receiverChatRoomId', ''); 
      formData.append('id', id);

      const response = await axios.post(FILE_UPLOAD_URL, formData, { headers: { 'Content-Type': 'multipart/form-data' } }); 
      return response.data.fileUrl; 
  };

  const sendDirectMessage = async () => {
    if ((!message.trim() && !selectedFile) || !selectedUser || !user) return;
    
    const msgId = generateId();

    if (selectedFile) {
        const tempUrl = URL.createObjectURL(selectedFile);
        const fileName = selectedFile.name;
        const fileToUpload = selectedFile;

        const fileMessage: PayloadMessage = { 
             senderName: user.username, 
             receiverName: selectedUser, 
             receiverChatRoomId: "", 
             content: fileName, 
             date: new Date().toISOString(), 
             fileUrl: tempUrl, 
             fileName: fileName, 
             messageType: 'FILE',
             id: msgId 
        };
        
        setConversations(prev => ({ ...prev, [selectedUser]: [...(prev[selectedUser]||[]), fileMessage] })); 
        setMessage(""); 
        setSelectedFile(null); 
        
        try { 
             await handleFileUpload(fileToUpload, msgId); 
        } catch (error) { console.error(error); }
        return; 
    }

    const content = message.trim(); 
    setMessage("");
    
    const payloadMessage: PayloadMessage = { 
        senderName: user.username, 
        receiverName: selectedUser, 
        receiverChatRoomId: "", 
        content, 
        date: new Date().toISOString(), 
        messageType: 'TEXT',
        id: msgId 
    };
    
    setConversations(prev => ({ ...prev, [selectedUser]: [...(prev[selectedUser]||[]), payloadMessage] }));
    
    if (stompClient?.connected) stompClient.publish({ destination: "/app/private-message", body: JSON.stringify(payloadMessage) });
  };

  const handleCloseDialog = () => { setIsDialogOpen(false); setSelectedUser(""); setActiveMessages([]); };
  const handleSelectConversation = (username: string) => { openDirectMessage(username); };
  const removeSelectedFile = () => { setSelectedFile(null); };
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => { if (event.target.files?.[0]) setSelectedFile(event.target.files[0]); };

  return (
    <Box sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" sx={{ mb: 2 }}>Direct Messages</Typography>
      <Box sx={{ mb: 2 }}><Button variant="contained" onClick={() => setIsUserSelectOpen(true)} sx={{ mb: 2 }}>Choose User to Message</Button></Box>

      <DMConversationsList
        user={user}
        conversations={conversations}
        onSelectConversation={handleSelectConversation}
        userAvatars={userAvatars}
        selectedUser={selectedUser}
        unreadPerUser={unreadPerUser}
      />

      <Dialog open={isUserSelectOpen} onClose={() => setIsUserSelectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Select User</DialogTitle>
        <DialogContent><List>{(allUsers || []).filter(u => u !== user?.username).map((username) => (<ListItem key={username} onClick={() => { openDirectMessage(username); setIsUserSelectOpen(false); }} sx={{ cursor: 'pointer' }}><Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}><Avatar avatarUrl={userAvatars[username] || "http://localhost:8081/avatars/cat.png"} username={username} size={40} /><Typography>{username}</Typography></Box></ListItem>))}</List></DialogContent>
        <DialogActions><Button onClick={() => setIsUserSelectOpen(false)}>Close</Button></DialogActions>
      </Dialog>

      <Dialog open={isDialogOpen} onClose={handleCloseDialog} maxWidth="md" fullWidth PaperProps={{ sx: { bgcolor: 'background.paper', height: '80vh' } }}>
        <DialogTitle>Chat with {selectedUser}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', p: 2 }}>
          <Paper sx={{ flex: 1, mb: 2, p: 2, overflowY: 'auto', bgcolor: '#f5f5f5' }}>
            <List>
              {Array.isArray(activeMessages) && activeMessages.length === 0 && <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 2 }}>No messages yet. Say hello!</Typography>}
              {Array.isArray(activeMessages) && activeMessages.map((msg, index) => {
                if (!msg) return null;
                const isMe = msg.senderName === user?.username;
                const avatar = userAvatars[msg.senderName] || "http://localhost:8081/avatars/cat.png";
                const fileInfo = extractFileInfo(msg);
                const getFullUrl = (url: string) => url.startsWith('http') || url.startsWith('blob:') ? url : `http://localhost:8081${url}`;
                
                return (
                  <ListItem key={index} sx={{ p: 0, mb: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: isMe ? 'flex-end' : 'flex-start', width: '100%', gap: 1 }}>
                      {!isMe && <Avatar avatarUrl={avatar} username={msg.senderName} size={32} />}
                      <Box sx={{ maxWidth: '70%', p: 1.5, borderRadius: 2, bgcolor: isMe ? '#1976d2' : '#ffffff', color: isMe ? 'white' : 'black', boxShadow: 1 }}>
                        {fileInfo && (<Box sx={{ mb: 1 }}>{fileInfo.isImage ? <Box component="img" src={getFullUrl(fileInfo.fileUrl)} alt={fileInfo.fileName} sx={{ maxWidth: '100%', maxHeight: 200, borderRadius: 1, cursor: 'pointer', '&:hover': { opacity: 0.8 } }} onClick={() => window.open(getFullUrl(fileInfo.fileUrl), '_blank')} /> : <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1, bgcolor: 'rgba(0,0,0,0.1)', borderRadius: 1, cursor: 'pointer', '&:hover': { bgcolor: 'rgba(0,0,0,0.2)' } }} onClick={() => window.open(getFullUrl(fileInfo.fileUrl), '_blank')}><DescriptionIcon sx={{ fontSize: 20 }} /><Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{fileInfo.fileName}</Typography></Box>}</Box>)}
                        {msg.content && !fileInfo && <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>{msg.content}</Typography>}
                        <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7, fontSize: '0.7rem', textAlign: isMe ? 'right' : 'left' }}>{safeFormatDate(msg.date)}</Typography>
                      </Box>
                    </Box>
                  </ListItem>
                );
              })}
              <div ref={messagesEndRef} />
            </List>
          </Paper>
          {selectedFile && <Box sx={{ mb: 1, p: 1, bgcolor: '#f5f5f5', borderRadius: 1, display: 'flex', alignItems: 'center', gap: 1 }}>{selectedFile.type.startsWith('image/') ? <ImageIcon color="primary" /> : <DescriptionIcon color="primary" />}<Typography variant="body2" sx={{ flex: 1, wordBreak: 'break-all' }}>{selectedFile.name}</Typography><IconButton size="small" onClick={removeSelectedFile}>×</IconButton></Box>}
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField fullWidth variant="outlined" placeholder="Type a message..." value={message} onChange={(e) => setMessage(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && sendDirectMessage()} InputProps={{ startAdornment: (<InputAdornment position="start"><input type="file" id="dm-file-upload" style={{ display: 'none' }} onChange={handleFileSelect} accept="image/*,.pdf,.doc,.docx,.txt" /><IconButton size="small" onClick={() => document.getElementById('dm-file-upload')?.click()}><AttachFileIcon /></IconButton></InputAdornment>) }} />
            <Button variant="contained" onClick={sendDirectMessage} disabled={!message.trim() && !selectedFile}>Send</Button>
          </Box>
        </DialogContent>
        <DialogActions><Button onClick={handleCloseDialog}>Close</Button></DialogActions>
      </Dialog>
    </Box>
  );
};

export default DirectMessages;