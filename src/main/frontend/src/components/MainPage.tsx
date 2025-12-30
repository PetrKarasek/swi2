import { Box, Button, TextField, Typography, Paper, List, ListItem, AppBar, Toolbar, Tabs, Tab } from "@mui/material";
import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, UserToken } from "../types";
import DirectMessages from "./DirectMessages";
import Avatar from "./Avatar";
import AvatarSelector from "./AvatarSelector";

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";
const HISTORY_URL = "http://localhost:8081/api/history";
const USERS_URL = "http://localhost:8081/users";

const PENDING_MESSAGES_KEY = "pendingMessages";

const MainPage = (props: { user: UserToken | null; setUserToken: (token: UserToken | null | string) => void }) => {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<PayloadMessage[]>([]);
  const [connected, setConnected] = useState(false);
  const [pendingMessages, setPendingMessages] = useState<PayloadMessage[]>([]);
  const [allUsers, setAllUsers] = useState<string[]>([]);
  const [currentTab, setCurrentTab] = useState(0);
  const [userAvatar, setUserAvatar] = useState<string>("");
  const [avatarSelectorOpen, setAvatarSelectorOpen] = useState(false);
  const isLoggedIn = !!props.user;

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Load pending messages from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(PENDING_MESSAGES_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setPendingMessages(parsed);
        // Show pending messages immediately in UI for non-logged users
        if (!isLoggedIn) {
          setMessages(parsed);
          console.log("Loaded pending messages to UI:", parsed.length);
        }
      } catch (e) {
        console.error("Error parsing pending messages:", e);
      }
    }
  }, [isLoggedIn]);

  // Function to send pending messages when user logs in and WebSocket is connected
  const sendPendingMessages = () => {
    console.log("sendPendingMessages called, isLoggedIn:", isLoggedIn, "user:", !!props.user);
    if (!isLoggedIn || !props.user) {
      console.log("Cannot send pending messages - not logged in");
      return;
    }
    
    const stored = localStorage.getItem(PENDING_MESSAGES_KEY);
    if (!stored) {
      console.log("No pending messages found in storage");
      return;
    }
    
    try {
      const pending = JSON.parse(stored);
      console.log("Found pending messages:", pending.length);
      if (!Array.isArray(pending) || pending.length === 0) {
        console.log("No pending messages to send");
        return;
      }
      
      const client = stompClientRef.current;
      if (client && client.connected) {
        console.log("WebSocket is connected, sending pending messages...");
        pending.forEach((msg: PayloadMessage, index: number) => {
          // Update sender to current user
          const updatedMsg = {
            ...msg,
            senderName: props.user!.username,
            date: new Date().toISOString() // Update timestamp
          };
          
          console.log(`Sending pending message ${index + 1}:`, updatedMsg);
          client.publish({
            destination: "/app/message",
            body: JSON.stringify(updatedMsg),
          });
          
          // Add message to local state immediately
          setMessages((prev) => {
            console.log("Adding sent pending message to local state");
            return [...prev, updatedMsg];
          });
        });
        
        // Clear pending messages after sending
        localStorage.removeItem(PENDING_MESSAGES_KEY);
        setPendingMessages([]);
        console.log("Pending messages sent and cleared");
      } else {
        console.log("WebSocket not connected, cannot send pending messages");
      }
    } catch (e) {
      console.error("Error sending pending messages:", e);
    }
  };

  // When user logs in, try to send pending messages and load users
  useEffect(() => {
    if (isLoggedIn && connected) {
      sendPendingMessages();
      loadUsers();
      loadUserAvatar(); // Load user's avatar
    }
  }, [isLoggedIn, connected]);

  // Also load avatar when user changes
  useEffect(() => {
    if (props.user) {
      loadUserAvatar();
    }
  }, [props.user]);

  useEffect(() => {
    let sock = new SockJS("http://localhost:8081/ws");
    let stompClient = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: (str: string) => console.log(str),
      onConnect: () => {
        console.log("Connected to WebSocket");
        setConnected(true);
        console.log("Attempting to subscribe to /chatroom/1");
        const subscription = stompClient.subscribe("/chatroom/1", (message: any) => {
          console.log("Received message on /chatroom/1:", message);
          try {
            const payloadData: PayloadMessage = JSON.parse(message.body);
            console.log("Message received from: " + payloadData.senderName);
            console.log("Full message data:", payloadData);
            
            // Use functional update to avoid closure issues
            setMessages((prev: PayloadMessage[]) => {
              console.log("Adding message to existing messages:", prev.length);
              
              // Check if this is a duplicate message (sender receiving their own message)
              // But don't check for pending messages that were just sent
              const isDuplicate = prev.length > 0 && 
                prev[prev.length - 1].senderName === payloadData.senderName &&
                prev[prev.length - 1].content === payloadData.content &&
                Math.abs(new Date(prev[prev.length - 1].date).getTime() - new Date(payloadData.date).getTime()) < 1000; // Only check duplicates within 1 second
              
              if (isDuplicate) {
                console.log("Skipping duplicate message from:", payloadData.senderName);
                return prev;
              }
              
              const newMessages = [...prev, payloadData];
              console.log("New messages count:", newMessages.length);
              return newMessages;
            });
          } catch (error) {
            console.error("Error parsing message:", error);
          }
        });
        console.log("Successfully subscribed to /chatroom/1");
        
        // Send pending messages when WebSocket connects and user is logged in
        if (isLoggedIn && props.user) {
          console.log("WebSocket connected, sending pending messages...");
          sendPendingMessages();
        }
        
        // If user is logged in and we have pending messages, send them now
        setTimeout(() => {
          sendPendingMessages();
        }, 500); // Small delay to ensure subscription is ready
      },
      onDisconnect: () => {
        console.log("Disconnected from WebSocket");
        setConnected(false);
      },
      onStompError: (frame: any) => {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
        setConnected(false);
      },
    });

    stompClientRef.current = stompClient;
    stompClient.activate();

    // Nejprve načteme historii z databáze, pak případně zprávy z fronty (pro dobu, kdy byl uživatel odhlášen).
    loadHistory().then(() => {
      if (props.user) {
        pickupMessages();
      }
    });

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    // Force re-render when messages change
    if (messages.length > 0) {
      console.log("Messages updated, count:", messages.length);
    }
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  async function loadUsers() {
    try {
      const result = await axios.get<string[]>(USERS_URL);
      if (result.data) {
        setAllUsers(result.data);
        console.log("Loaded users:", result.data);
      }
    } catch (e) {
      console.log("Error loading users:", e);
      // Fallback users for testing
      setAllUsers(["testuser", "alice", "bob"]);
    }
  }

  async function loadUserAvatar() {
    try {
      // Get user info including avatar
      const response = await axios.get(`http://localhost:8081/api/user/${props.user?.username}`);
      if (response.data && response.data.avatarUrl) {
        setUserAvatar(response.data.avatarUrl);
        console.log("Loaded user avatar:", response.data.avatarUrl);
      }
    } catch (e) {
      console.log("Error loading user avatar:", e);
      // Set default avatar if none found
      setUserAvatar("http://localhost:8081/avatars/cat.png");
    }
  }

  async function loadHistory() {
    console.log("loadHistory called, isLoggedIn:", isLoggedIn);
    try {
      const params = new URLSearchParams([
        ["chatRoomId", "1"],
        ["username", props.user?.username || ""]
      ]);
      const result = await axios.get<PayloadMessage[]>(HISTORY_URL, { params });
      if (result.data) {
        console.log("History loaded from server:", result.data.length);
        // If not logged in, combine history with pending messages
        if (!isLoggedIn) {
          const stored = localStorage.getItem(PENDING_MESSAGES_KEY);
          let pendingMessages: PayloadMessage[] = [];
          if (stored) {
            try {
              pendingMessages = JSON.parse(stored);
              console.log("Found pending messages in storage:", pendingMessages.length);
            } catch (e) {
              console.error("Error parsing pending messages:", e);
            }
          }
          
          // Combine history and pending messages, but don't override existing messages
          setMessages((prevMessages) => {
            console.log("Current messages before combining:", prevMessages.length);
            const existingPending = prevMessages.filter(msg => msg.senderName === "Guest");
            console.log("Existing pending messages:", existingPending.length);
            const allMessages = [...result.data, ...existingPending];
            console.log("Combined history + pending messages:", allMessages.length);
            return allMessages;
          });
        } else {
          // Logged in users only see history
          setMessages(result.data);
          console.log("Loaded message history for logged user:", result.data.length);
        }
      }
    } catch (e) {
      console.log("Error loading history:", e);
      // If error and not logged in, try to show pending messages
      if (!isLoggedIn) {
        const stored = localStorage.getItem(PENDING_MESSAGES_KEY);
        if (stored) {
          try {
            const pendingMessages = JSON.parse(stored);
            setMessages(pendingMessages);
            console.log("Showing pending messages due to history error:", pendingMessages.length);
          } catch (e) {
            console.error("Error parsing pending messages:", e);
          }
        }
      }
    }
  }

  function sendMessage() {
    if (!message.trim()) return;

    const trimmedMessage = message.trim();
    setMessage("");

    if (isLoggedIn && props.user) {
      // Logged in user: send immediately via WebSocket
      const client = stompClientRef.current;
      if (client && client.connected) {
        const payloadMessage: PayloadMessage = {
          senderName: props.user.username,
          receiverChatRoomId: "1",
          content: trimmedMessage,
          date: new Date().toISOString(),
        };

        // Add message to local state immediately for instant feedback
        setMessages((prev) => {
          console.log("Adding own message to local state:", prev.length);
          return [...prev, payloadMessage];
        });

        client.publish({
          destination: "/app/message",
          body: JSON.stringify(payloadMessage),
        });
        console.log("Message sent to /app/message:", payloadMessage);
      } else {
        console.error("WebSocket not connected");
      }
    } else {
      // Non-logged user: store in localStorage queue and show immediately
      const pendingMsg: PayloadMessage = {
        senderName: "Guest",
        receiverChatRoomId: "1",
        content: trimmedMessage,
        date: new Date().toISOString(),
      };
      
      // Add to pending messages queue
      setPendingMessages((prev) => {
        const updated = [...prev, pendingMsg];
        localStorage.setItem(PENDING_MESSAGES_KEY, JSON.stringify(updated));
        return updated;
      });
      
      // Show message immediately in UI
      setMessages((prev) => {
        console.log("Adding queued message to UI:", prev.length);
        const newMessages = [...prev, pendingMsg];
        console.log("New messages count after adding:", newMessages.length);
        return newMessages;
      });
    }
  }

  async function pickupMessages() {
    if (!props.user) return;
    
    // Just reload the message history when user logs in
    await loadHistory();
  }

  function logout() {
    if (stompClientRef.current && stompClientRef.current.active) {
      stompClientRef.current.deactivate();
      console.log("Disconnected from WebSocket");
    }
    props.setUserToken("");
  }

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    console.log('Key pressed:', e.key);
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleMessageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log('Message change:', e.target.value);
    setMessage(e.target.value);
  };

  const handleAvatarClick = () => {
    if (isLoggedIn) {
      setAvatarSelectorOpen(true);
    }
  };

  const handleAvatarChange = (newAvatarUrl: string) => {
    setUserAvatar(newAvatarUrl);
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" sx={{ background: "rgba(15,23,42,0.98)" }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Chat Application · {isLoggedIn ? props.user?.username : "Host"}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {isLoggedIn && (
              <Avatar 
                avatarUrl={userAvatar}
                username={props.user?.username}
                onClick={handleAvatarClick}
                clickable={true}
              />
            )}
            <Typography variant="body2">
              {connected ? "🟢 Connected" : "🔴 Disconnected"}
            </Typography>
            {isLoggedIn && (
              <Button color="inherit" onClick={logout}>
                Logout
              </Button>
            )}
          </Box>
        </Toolbar>
      </AppBar>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={currentTab} onChange={(e, newValue) => setCurrentTab(newValue)}>
          <Tab label="Public Chat" />
          <Tab label="Direct Messages" />
        </Tabs>
      </Box>

      {currentTab === 0 && (
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2, position: 'relative', overflow: 'hidden' }}>
          <Paper 
            sx={{ 
              flex: 1, 
              p: 2, 
              mb: 2, 
              overflow: 'auto',
              backgroundColor: '#f5f5f5',
              maxHeight: 'calc(100% - 80px)'
            }}
          >
            <List>
              {messages.map((msg: PayloadMessage, index: number) => (
                <ListItem key={index} sx={{ p: 0, mb: 1 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      justifyContent: msg.senderName === props.user?.username ? 'flex-end' : 'flex-start',
                      width: '100%',
                      gap: 1
                    }}
                  >
                    {msg.senderName !== props.user?.username && (
                      <Avatar 
                        avatarUrl={msg.senderAvatarUrl}
                        username={msg.senderName}
                        size={32}
                      />
                    )}
                    <Box
                      sx={{
                        maxWidth: '70%',
                        p: 2,
                        borderRadius: 2,
                        backgroundColor: msg.senderName === props.user?.username ? '#1976d2' : '#e0e0e0',
                        color: msg.senderName === props.user?.username ? 'white' : 'black'
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
                    {msg.senderName === props.user?.username && (
                      <Avatar 
                        avatarUrl={userAvatar}
                        username={msg.senderName}
                        size={32}
                      />
                    )}
                  </Box>
                </ListItem>
              ))}
              <div ref={messagesEndRef} />
            </List>
          </Paper>

          <Box sx={{ 
            position: 'absolute', 
            bottom: 0, 
            left: 0, 
            right: 0, 
            p: 2, 
            backgroundColor: 'white',
            borderTop: '1px solid #e0e0e0'
          }}>
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
            <Box sx={{ flex: 1 }}>
              <TextField
                fullWidth
                label={isLoggedIn ? "Type your message here" : pendingMessages.length > 0 ? `${pendingMessages.length} zpráv ve frontě - přihlas se pro odeslání` : "Napiš zprávu (bude ve frontě do přihlášení)"}
                variant="outlined"
                value={message}
                onChange={handleMessageChange}
                onKeyPress={handleKeyPress}
                disabled={false}
                inputProps={{ style: { pointerEvents: 'auto' } }}
              />
              {pendingMessages.length > 0 && !isLoggedIn && (
                <Typography variant="caption" sx={{ color: '#1976d2', mt: 0.5, display: 'block' }}>
                  {pendingMessages.length} zpráv čeká ve frontě. Přihlas se pro jejich odeslání.
                </Typography>
              )}
            </Box>
            <Button 
              variant="contained" 
              onClick={sendMessage}
              disabled={!message.trim()}
              sx={{ minWidth: 80 }}
            >
              Send
            </Button>
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
          />
        </Box>
      )}
      
      <AvatarSelector
        open={avatarSelectorOpen}
        onClose={() => setAvatarSelectorOpen(false)}
        username={props.user?.username || ""}
        currentAvatar={userAvatar}
        onAvatarChange={handleAvatarChange}
      />
    </Box>
  );
};

export default MainPage;
