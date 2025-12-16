import { Box, Button, TextField, Typography, Paper, List, ListItem, AppBar, Toolbar } from "@mui/material";
import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { PayloadMessage, User, UserToken } from "../types";

var stompClient: any = null;
const PICKUP_URL = "http://localhost:8081/api/queue";

const MainPage = (props: { user: User; setUserToken: (token: UserToken | null | string) => void }) => {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<PayloadMessage[]>([]);
  const [connected, setConnected] = useState(false);

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

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

    pickupMessages();

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
    let payloadData: PayloadMessage = JSON.parse(payload.body);
    console.log("Message received from: " + payloadData.senderName);
    setMessages((prev: PayloadMessage[]) => [...prev, payloadData]);
    pickupMessages();
  }

  function sendMessage() {
    if (!message.trim()) return;

    const client = stompClientRef.current;
    if (client && client.connected) {
      const payloadMessage: PayloadMessage = {
        senderName: props.user.username,
        receiverChatRoomId: "1",
        content: message.trim(),
        date: new Date().toISOString(),
      };

      client.publish({
        destination: "/app/message",
        body: JSON.stringify(payloadMessage),
      });

      setMessage("");
    } else {
      console.error("WebSocket not connected");
    }
  }

  async function pickupMessages() {
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
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" sx={{ background: "rgba(15,23,42,0.98)" }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Hlavní místnost · {props.user.username}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="body2">
              {connected ? "🟢 Connected" : "🔴 Disconnected"}
            </Typography>
            <Button color="inherit" onClick={logout}>
              Logout
            </Button>
          </Box>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2 }}>
        <Paper 
          sx={{ 
            flex: 1, 
            p: 2, 
            mb: 2, 
            overflow: 'auto',
            backgroundColor: '#f5f5f5'
          }}
        >
          <List>
            {messages.map((msg: PayloadMessage, index: number) => (
              <ListItem key={index} sx={{ p: 0, mb: 1 }}>
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: msg.senderName === props.user.username ? 'flex-end' : 'flex-start',
                    width: '100%'
                  }}
                >
                  <Box
                    sx={{
                      maxWidth: '70%',
                      p: 2,
                      borderRadius: 2,
                      backgroundColor: msg.senderName === props.user.username ? '#1976d2' : '#e0e0e0',
                      color: msg.senderName === props.user.username ? 'white' : 'black'
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

        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField
            fullWidth
            label="Type your message here"
            variant="outlined"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={!connected}
          />
          <Button 
            variant="contained" 
            onClick={sendMessage}
            disabled={!connected || !message.trim()}
            sx={{ minWidth: 80 }}
          >
            Send
          </Button>
        </Box>
      </Box>
    </Box>
  );
};

export default MainPage;
