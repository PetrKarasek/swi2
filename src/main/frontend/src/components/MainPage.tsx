import { Box, Button, TextField, Typography, Paper, List, ListItem, AppBar, Toolbar } from "@mui/material";
import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, UserToken } from "../types";

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";
const HISTORY_URL = "http://localhost:8081/api/history";

const PENDING_MESSAGES_KEY = "pendingMessages";

const MainPage = (props: { user: UserToken | null; setUserToken: (token: UserToken | null | string) => void }) => {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<PayloadMessage[]>([]);
  const [connected, setConnected] = useState(false);
  const [pendingMessages, setPendingMessages] = useState<PayloadMessage[]>([]);
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
      } catch (e) {
        console.error("Error parsing pending messages:", e);
      }
    }
  }, []);

  // Function to send pending messages when user logs in and WebSocket is connected
  const sendPendingMessages = () => {
    if (!isLoggedIn || !props.user) return;
    
    const stored = localStorage.getItem(PENDING_MESSAGES_KEY);
    if (!stored) return;
    
    try {
      const pending = JSON.parse(stored);
      if (!Array.isArray(pending) || pending.length === 0) return;
      
      const client = stompClientRef.current;
      if (client && client.connected) {
        // Send all pending messages
        pending.forEach((pendingMsg: PayloadMessage) => {
          const payloadMessage: PayloadMessage = {
            senderName: props.user!.username,
            receiverChatRoomId: pendingMsg.receiverChatRoomId,
            content: pendingMsg.content,
            date: pendingMsg.date,
          };
          client.publish({
            destination: "/app/message",
            body: JSON.stringify(payloadMessage),
          });
        });
        // Clear pending messages
        localStorage.removeItem(PENDING_MESSAGES_KEY);
        setPendingMessages([]);
      }
    } catch (e) {
      console.error("Error sending pending messages:", e);
    }
  };

  // When user logs in, try to send pending messages
  useEffect(() => {
    if (isLoggedIn && connected) {
      sendPendingMessages();
    }
  }, [isLoggedIn, connected, props.user]);

  useEffect(() => {
    let sock = new SockJS("http://localhost:8081/ws");
    stompClient = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: (str: string) => console.log(str),
      onConnect: () => {
        console.log("Connected to WebSocket");
        setConnected(true);
        stompClient.subscribe("/chatroom/1", onPublicMessageReceived);
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

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  function onPublicMessageReceived(payload: any) {
    // Zprávy přijaté v reálném čase pouze přidáme do seznamu.
    let payloadData: PayloadMessage = JSON.parse(payload.body);
    console.log("Message received from: " + payloadData.senderName);
    setMessages((prev: PayloadMessage[]) => [...prev, payloadData]);
  }

  async function loadHistory() {
    try {
      const params = new URLSearchParams([["chatRoomId", "1"]]);
      const result = await axios.get<PayloadMessage[]>(HISTORY_URL, { params });
      if (result.data) {
        setMessages(result.data);
      }
    } catch (e) {
      console.log("Error loading history:", e);
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

        client.publish({
          destination: "/app/message",
          body: JSON.stringify(payloadMessage),
        });
      } else {
        console.error("WebSocket not connected");
      }
    } else {
      // Non-logged user: store in localStorage queue
      const pendingMsg: PayloadMessage = {
        senderName: "Guest",
        receiverChatRoomId: "1",
        content: trimmedMessage,
        date: new Date().toISOString(),
      };
      setPendingMessages((prev) => {
        const updated = [...prev, pendingMsg];
        localStorage.setItem(PENDING_MESSAGES_KEY, JSON.stringify(updated));
        return updated;
      });
    }
  }

  async function pickupMessages() {
    if (!props.user) return;
    const params = new URLSearchParams([["userId", props.user.userId]]);

    try {
      const result = await axios.get<PayloadMessage[]>(PICKUP_URL, { params });
      
      if (result.data && result.data.length > 0) {
        setMessages((prev: PayloadMessage[]) => [...prev, ...result.data]);
      }
    } catch (e) {
      console.log("Error picking up messages:", e);
    }
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

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" sx={{ background: "rgba(15,23,42,0.98)" }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Hlavní místnost · {isLoggedIn ? props.user?.username : "Host"}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
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
                    justifyContent: msg.senderName === props.user?.username ? 'flex-end' : 'flex-start',
                    width: '100%'
                  }}
                >
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
    </Box>
  );
};

export default MainPage;
