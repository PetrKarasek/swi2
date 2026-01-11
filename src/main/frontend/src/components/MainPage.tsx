import React, { useEffect, useState, useRef, useCallback } from "react";
import { 
  Box, Button, TextField, Typography, Paper, List, ListItem, 
  AppBar, Toolbar, Tabs, Tab, IconButton, InputAdornment
} from "@mui/material";
import AttachFileIcon from '@mui/icons-material/AttachFile';
import ImageIcon from '@mui/icons-material/Image';
import DescriptionIcon from '@mui/icons-material/Description';
import SockJS from "sockjs-client";
import { Client, IMessage } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, UserToken } from "../types";
import DirectMessages from "./DirectMessages";
import Avatar from "./Avatar";
import AvatarSelector from "./AvatarSelector";

const SOCKET_URL = "http://localhost:8081/ws";
const HISTORY_URL = "http://localhost:8081/api/history";
const USERS_URL = "http://localhost:8081/users";
const USER_API_URL = "http://localhost:8081/api/user";
const FILE_UPLOAD_URL = "http://localhost:8081/api/upload-file";
const UNREAD_MSGS_URL = "http://localhost:8081/api/unread-messages";
const PENDING_MESSAGES_KEY = "pendingMessages";
const DEFAULT_AVATAR = "http://localhost:8081/avatars/cat.png";

const extractFileInfo = (msg: PayloadMessage) => {
  if (msg.fileUrl) return { fileUrl: msg.fileUrl, fileName: msg.fileName || 'download', isImage: msg.fileName?.match(/\.(jpg|jpeg|png|gif|webp)$/i) };
  if (msg.content && typeof msg.content === 'string') {
    const match = msg.content.match(/^\s*\[FILE\]\s*(.*?)\s*\|\s*(.*)$/);
    if (match && match.length >= 3) return { fileUrl: match[2].trim(), fileName: match[1].trim(), isImage: match[1].match(/\.(jpg|jpeg|png|gif|webp)$/i) };
  }
  return null;
};

const TabLabelWithBadge = ({ label, count }: { label: string, count: number }) => (
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
    <span>{label}</span>
    {count > 0 && <Box sx={{ bgcolor: 'error.main', color: 'white', borderRadius: '50%', width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 'bold' }}>{count}</Box>}
  </Box>
);

const ChatMessageItem = ({ msg, isOwnMessage, userAvatar }: { msg: PayloadMessage, isOwnMessage: boolean, userAvatar: string }) => {
  const fileInfo = extractFileInfo(msg);
  const getFullUrl = (url: string) => url.startsWith('http') ? url : `http://localhost:8081${url}`;
  return (
    <ListItem sx={{ p: 0, mb: 1 }}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', width: '100%', gap: 1, justifyContent: isOwnMessage ? 'flex-end' : 'flex-start' }}>
        {!isOwnMessage && <Avatar avatarUrl={msg.senderAvatarUrl || DEFAULT_AVATAR} username={msg.senderName} size={32} />}
        <Box sx={{ maxWidth: '70%', p: 2, borderRadius: 2, bgcolor: isOwnMessage ? '#1976d2' : '#e0e0e0', color: isOwnMessage ? 'white' : 'black' }}>
          <Typography variant="caption" sx={{ display: 'block', mb: 0.5 }}>{isOwnMessage ? 'You' : msg.senderName}</Typography>
          {fileInfo && (
            <Box sx={{ mb: 1 }}>
              {fileInfo.isImage ? 
                <Box component="img" src={getFullUrl(fileInfo.fileUrl)} alt={fileInfo.fileName} sx={{ maxWidth: '100%', maxHeight: 200, borderRadius: 1, cursor: 'pointer', '&:hover': { opacity: 0.8 } }} onClick={() => window.open(getFullUrl(fileInfo.fileUrl), '_blank')} /> :
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1, bgcolor: 'rgba(0,0,0,0.1)', borderRadius: 1, cursor: 'pointer', '&:hover': { bgcolor: 'rgba(0,0,0,0.2)' } }} onClick={() => window.open(getFullUrl(fileInfo.fileUrl), '_blank')}>
                  <DescriptionIcon /><Typography variant="body2" sx={{ wordBreak: 'break-all' }}>{fileInfo.fileName}</Typography>
                </Box>
              }
            </Box>
          )}
          {msg.content && !fileInfo && <Typography variant="body1">{msg.content}</Typography>}
          <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7 }}>{new Date(msg.date).toLocaleTimeString()}</Typography>
        </Box>
        {isOwnMessage && <Avatar avatarUrl={userAvatar} username={msg.senderName} size={32} />}
      </Box>
    </ListItem>
  );
};

const MainPage = (props: { user: UserToken | null; setUserToken: (token: UserToken | null | string) => void }) => {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<PayloadMessage[]>([]);
  const [connected, setConnected] = useState(false);
  const [pendingMessages, setPendingMessages] = useState<PayloadMessage[]>([]);
  const [allUsers, setAllUsers] = useState<string[]>([]);
  const [currentTab, setCurrentTab] = useState(0);
  const [userAvatar, setUserAvatar] = useState<string>("");
  const [avatarSelectorOpen, setAvatarSelectorOpen] = useState(false);
  const [unreadPublicMessages, setUnreadPublicMessages] = useState(0);
  const [unreadDMs, setUnreadDMs] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [conversations, setConversations] = useState<{ [username: string]: PayloadMessage[] }>({});
  const [unreadPerUser, setUnreadPerUser] = useState<{[key: string]: number}>({});

  const isLoggedIn = !!props.user;
  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const processedMessagesRef = useRef(new Set<string>());

  const scrollToBottom = () => { messagesEndRef.current?.scrollIntoView({ behavior: "smooth" }); };

  const loadUserAvatar = useCallback(async () => {
    if (!props.user) return;
    try {
      const response = await axios.get(`${USER_API_URL}/${props.user.username}`);
      if (response.data?.avatarUrl) setUserAvatar(response.data.avatarUrl);
    } catch (e) { setUserAvatar(DEFAULT_AVATAR); }
  }, [props.user]);

  const loadUsers = useCallback(async () => {
    try {
      const result = await axios.get<string[]>(USERS_URL);
      if (result.data) {
        setAllUsers(result.data);
        localStorage.setItem('allUsers', JSON.stringify(result.data));
      }
    } catch (e) { setAllUsers(["user1", "user2"]); }
  }, []);

  const restoreConversations = useCallback(async () => {
    if (!props.user) return;
    try {
        const savedConversations = localStorage.getItem('dm_conversations');
        let newConvs: {[key: string]: PayloadMessage[]} = {};
        
        if (savedConversations) {
            newConvs = JSON.parse(savedConversations);
        }

        const res = await axios.get<PayloadMessage[]>(UNREAD_MSGS_URL, { params: { username: props.user.username } });
        if (res.data) {
            setUnreadDMs(res.data.length);
            
            const counts: {[key: string]: number} = {};
            
            res.data.forEach(msg => {
                counts[msg.senderName] = (counts[msg.senderName] || 0) + 1;

                const other = msg.senderName === props.user?.username ? msg.receiverName : msg.senderName;
                if (!other) return;
                if (!newConvs[other]) newConvs[other] = [];
                // Přidáme pouze pokud tam zpráva ještě není
                if (!newConvs[other].some((m: PayloadMessage) => m.date === msg.date)) {
                    newConvs[other].push(msg);
                }
            });
            
            setUnreadPerUser(counts);
            setConversations(newConvs);
        } else {
             // Pokud nejsou žádné unread messages, alespoň nastavíme to, co bylo v localStorage
             setConversations(newConvs);
        }
    } catch (e) { console.error("Failed to restore DMs", e); }
  }, [props.user]);

  const clearUnreadForUser = (username: string) => {
    setUnreadPerUser(prev => {
        const newCounts = { ...prev };
        const countToRemove = newCounts[username] || 0;
        delete newCounts[username];
        setUnreadDMs(currentTotal => Math.max(0, currentTotal - countToRemove));
        return newCounts;
    });
  };

  useEffect(() => {
    if (isLoggedIn) {
        loadUsers();
        loadUserAvatar();
        restoreConversations();
    }
  }, [isLoggedIn, loadUsers, loadUserAvatar, restoreConversations]);

  // --- OPRAVA: Kontrola změny uživatele a vyčištění dat ---
  useEffect(() => {
    const savedUser = localStorage.getItem('lastUser');
    
    // Pokud se uživatel změnil (přihlásil se někdo jiný), vymažeme všechna citlivá data
    if (props.user?.username && savedUser && savedUser !== props.user.username) {
        console.log("Detecting user switch. Clearing old data.");
        localStorage.removeItem('dm_conversations');
        localStorage.removeItem('unreadDMs');
        localStorage.removeItem('unreadPublicMessages');
        localStorage.removeItem('currentTab');
        localStorage.removeItem(PENDING_MESSAGES_KEY);
        
        setConversations({});
        setUnreadPerUser({});
        setUnreadDMs(0);
        setUnreadPublicMessages(0);
        setCurrentTab(0);
    }

    // Načtení Pending messages (jen pokud je to stejný uživatel nebo guest)
    const storedPending = localStorage.getItem(PENDING_MESSAGES_KEY);
    if (storedPending && (!savedUser || savedUser === props.user?.username)) {
       setPendingMessages(JSON.parse(storedPending));
       if (!isLoggedIn) setMessages(JSON.parse(storedPending));
    }
    
    // Načtení stavu tabů (jen pokud je to stejný uživatel)
    if (savedUser === props.user?.username) {
        const savedTab = localStorage.getItem('currentTab');
        if (savedTab) setCurrentTab(parseInt(savedTab));
        const savedUnreadP = localStorage.getItem('unreadPublicMessages');
        if (savedUnreadP) setUnreadPublicMessages(parseInt(savedUnreadP));
    }

    if (props.user?.username) localStorage.setItem('lastUser', props.user.username);
  }, [isLoggedIn, props.user?.username]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const params = new URLSearchParams([["chatRoomId", "1"], ["username", props.user?.username || ""]]);
        const result = await axios.get<PayloadMessage[]>(HISTORY_URL, { params });
        setMessages(result.data); 
      } catch (e) { console.error(e); }
    };
    loadData();
  }, [isLoggedIn, props.user?.username]);

  // WebSocket
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(SOCKET_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        console.log("WebSocket Connected");
        
        client.subscribe("/chatroom/1", (message: IMessage) => {
          try {
            const payload: PayloadMessage = JSON.parse(message.body);
            const msgSignature = `${payload.senderName}-${payload.date}-${payload.content}`;
            if (processedMessagesRef.current.has(msgSignature)) return;
            processedMessagesRef.current.add(msgSignature);

            setMessages((prev) => {
              if (payload.senderName === props.user?.username) return prev;
              const recent = prev.slice(-3);
              const isDuplicate = recent.some(m => m.senderName === payload.senderName && m.content === payload.content && Math.abs(new Date(m.date).getTime() - new Date(payload.date).getTime()) < 1000);
              if (isDuplicate) return prev;
              
              if (localStorage.getItem('currentTab') !== '0') {
                 setUnreadPublicMessages(c => {
                     const newC = c + 1;
                     localStorage.setItem('unreadPublicMessages', newC.toString());
                     return newC;
                 });
              }
              return [...prev, payload];
            });
          } catch (e) { console.error(e); }
        });

        if (props.user?.username) {
          client.subscribe(`/user/${props.user.username}/private`, (message: IMessage) => {
              try {
                const payload = JSON.parse(message.body);
                const msgSignature = `${payload.senderName}-${payload.date}-${payload.content}`;
                if (processedMessagesRef.current.has(msgSignature)) return;
                processedMessagesRef.current.add(msgSignature);

                const otherParty = payload.senderName === props.user?.username ? payload.receiverName : payload.senderName;
                if (!otherParty) return;

                setConversations(prev => {
                    const currentMsgs = prev[otherParty] || [];
                    if (payload.senderName === props.user?.username) return prev;
                    const lastMsg = currentMsgs[currentMsgs.length - 1];
                    if (lastMsg && lastMsg.content === payload.content && Math.abs(new Date(lastMsg.date).getTime() - new Date(payload.date).getTime()) < 1000) return prev;

                    const updated = { ...prev, [otherParty]: [...currentMsgs, payload] };
                    localStorage.setItem('dm_conversations', JSON.stringify(updated));
                    return updated;
                });

                if (localStorage.getItem('currentTab') !== '1') {
                    setUnreadDMs(c => c + 1);
                    setUnreadPerUser(prev => ({
                        ...prev,
                        [payload.senderName]: (prev[payload.senderName] || 0) + 1
                    }));
                }
              } catch (e) { console.error(e); }
          });
        }
      },
      onDisconnect: () => setConnected(false)
    });

    client.activate();
    stompClientRef.current = client;
    return () => { if (client.connected) client.deactivate(); stompClientRef.current = null; };
  }, [isLoggedIn, props.user?.username]);

  useEffect(() => { scrollToBottom(); }, [messages, currentTab]);

  const sendMessage = async () => {
    if (!message.trim() && !selectedFile) return;
    const now = new Date().toISOString();

    if (selectedFile) {
      try {
        const uploadedFileUrl = await handleFileUpload(selectedFile);
        const fileName = selectedFile.name;
        const fileMsg: PayloadMessage = { senderName: props.user?.username || "Guest", receiverChatRoomId: "1", content: fileName, date: now, senderAvatarUrl: userAvatar, fileUrl: uploadedFileUrl, fileName: fileName, messageType: 'FILE' };
        setMessages(prev => [...prev, fileMsg]);
        setSelectedFile(null); setMessage("");
        return;
      } catch (error) { console.error(error); return; }
    }
    
    const content = message.trim();
    setMessage("");
    if (isLoggedIn && props.user) {
      const msg: PayloadMessage = { senderName: props.user.username, receiverChatRoomId: "1", content, date: now, senderAvatarUrl: userAvatar, fileUrl: undefined, fileName: undefined, messageType: 'TEXT' };
      setMessages(prev => [...prev, msg]);
      try { await axios.post("http://localhost:8081/api/message", msg); } catch (e) { console.error(e); }
    } else {
      const pendingMsg: PayloadMessage = { senderName: "Guest", receiverChatRoomId: "1", content, date: now, fileUrl: undefined, fileName: undefined, messageType: 'TEXT' };
      setMessages(prev => [...prev, pendingMsg]);
    }
  };

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
    localStorage.setItem('currentTab', newValue.toString());
    if (newValue === 0) {
        setUnreadPublicMessages(0);
        localStorage.setItem('unreadPublicMessages', '0');
    }
  };

  // --- OPRAVA: Funkce logout, která kompletně vyčistí data ---
  const logout = () => { 
    stompClientRef.current?.deactivate(); 
    
    // Vyčistit LocalStorage
    localStorage.removeItem('dm_conversations');
    localStorage.removeItem('unreadDMs');
    localStorage.removeItem('unreadPublicMessages');
    localStorage.removeItem('currentTab');
    localStorage.removeItem('pendingMessages');
    // localStorage.removeItem('lastUser'); // Necháme, aby se při příštím loginu poznala změna
    
    // Vyčistit State
    setConversations({});
    setUnreadPerUser({});
    setMessages([]);
    setUnreadDMs(0);
    setUnreadPublicMessages(0);
    setConnected(false);
    
    props.setUserToken(""); 
  };

  const handleFileUpload = async (file: File) => {
    if (!props.user) throw new Error('User not available');
    const formData = new FormData();
    formData.append('file', file);
    formData.append('username', props.user.username);
    formData.append('receiverChatRoomId', '1');
    try {
      const response = await axios.post(FILE_UPLOAD_URL, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      return response.data.fileUrl;
    } catch (error) { throw error; }
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) setSelectedFile(file);
  };

  const removeSelectedFile = () => {
    setSelectedFile(null);
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" sx={{ background: "rgba(15,23,42,0.98)" }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>Chat Application · {isLoggedIn ? props.user?.username : "Host"}</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {isLoggedIn && <Avatar avatarUrl={userAvatar} username={props.user?.username} onClick={() => setAvatarSelectorOpen(true)} clickable={true} />}
            <Typography variant="body2" sx={{ color: connected ? '#4caf50' : '#f44336', fontWeight: 'bold' }}>{connected ? "● Online" : "● Offline"}</Typography>
            {isLoggedIn && <Button color="inherit" onClick={logout}>Logout</Button>}
          </Box>
        </Toolbar>
      </AppBar>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={currentTab} onChange={handleTabChange}>
          <Tab label={<TabLabelWithBadge label="Public Chat" count={unreadPublicMessages} />} />
          <Tab label={<TabLabelWithBadge label="Direct Messages" count={unreadDMs} />} />
        </Tabs>
      </Box>
      {currentTab === 0 && (
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2, position: 'relative', overflow: 'hidden' }}>
          <Paper sx={{ flex: 1, p: 2, mb: 2, overflow: 'auto', backgroundColor: '#f5f5f5', maxHeight: 'calc(100% - 80px)' }}>
            <List>{messages.map((msg, index) => (<ChatMessageItem key={index} msg={msg} isOwnMessage={msg.senderName === props.user?.username} userAvatar={userAvatar} />))}<div ref={messagesEndRef} /></List>
          </Paper>
          <Box sx={{ position: 'absolute', bottom: 0, left: 0, right: 0, p: 2, backgroundColor: 'white', borderTop: '1px solid #e0e0e0' }}>
            {selectedFile && <Box sx={{ mb: 1, p: 1, bgcolor: '#f5f5f5', borderRadius: 1, display: 'flex', alignItems: 'center', gap: 1 }}>{selectedFile.name} <IconButton size="small" onClick={() => setSelectedFile(null)}>×</IconButton></Box>}
            <Box sx={{ display: 'flex', gap: 1 }}>
                <TextField fullWidth value={message} onChange={(e) => setMessage(e.target.value)} onKeyPress={(e) => e.key === 'Enter' && sendMessage()} InputProps={{ startAdornment: (<InputAdornment position="start"><input type="file" id="f" style={{display:'none'}} onChange={(e) => e.target.files && setSelectedFile(e.target.files[0])} /><IconButton onClick={() => document.getElementById('f')?.click()}><AttachFileIcon /></IconButton></InputAdornment>) }} />
                <Button variant="contained" onClick={sendMessage}>Send</Button>
            </Box>
          </Box>
        </Box>
      )}
      {currentTab === 1 && (
        <Box sx={{ flex: 1, p: 2 }}>
            <DirectMessages 
                user={props.user} 
                stompClient={stompClientRef.current} 
                allUsers={allUsers} 
                unreadDMs={unreadDMs} 
                onUnreadDMsChange={setUnreadDMs} 
                conversations={conversations} 
                setConversations={setConversations}
                unreadPerUser={unreadPerUser}
                clearUnreadForUser={clearUnreadForUser}
            />
        </Box>
      )}
      <AvatarSelector open={avatarSelectorOpen} onClose={() => setAvatarSelectorOpen(false)} username={props.user?.username || ""} currentAvatar={userAvatar} onAvatarChange={setUserAvatar} />
    </Box>
  );
};

export default MainPage;