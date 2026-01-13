import { Box, Button, TextField, Typography, Paper, List, ListItem, AppBar, Toolbar } from "@mui/material";
import React, { useState, useEffect } from "react";
import { PayloadMessage } from "../types";

const PENDING_MESSAGES_KEY = "pendingMessages";


const demoMessages: PayloadMessage[] = [
  {
    senderName: "alice",
    receiverChatRoomId: "1",
    content: "Vítej v hlavní místnosti 🎉",
    date: new Date().toISOString(),
  },
  {
    senderName: "bob",
    receiverChatRoomId: "1",
    content: "Přihlas se, aby ses připojil do konverzace.",
    date: new Date().toISOString(),
  },
];

const MainPagePreview: React.FC = () => {
  const [message, setMessage] = useState("");
  const [pendingMessages, setPendingMessages] = useState<PayloadMessage[]>([]);

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

  const sendMessage = () => {
    if (!message.trim()) return;

    const newMessage: PayloadMessage = {
      senderName: "Guest",
      receiverChatRoomId: "1",
      content: message.trim(),
      date: new Date().toISOString(),
    };

    const updatedPending = [...pendingMessages, newMessage];
    setPendingMessages(updatedPending);
    
    localStorage.setItem(PENDING_MESSAGES_KEY, JSON.stringify(updatedPending));
    
    setMessage("");
    
    console.log("Message queued for later:", newMessage);
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" sx={{ background: "#1976d2" }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Hlavní místnost · Nepřihlášený uživatel
          </Typography>
          <Typography variant="body2">
            🔒 Přihlas se pro čtení a odesílání zpráv
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2, backgroundColor: '#f3f4f6', position: 'relative', overflow: 'hidden' }}>
        <Paper
          sx={{
            flex: 1,
            p: 2,
            mb: 2,
            overflow: 'auto',
            backgroundColor: '#ffffff',
            maxHeight: 'calc(100% - 80px)'
          }}
        >
          <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>
            Zprávy mohou číst pouze přihlášení uživatelé.
            Níže je jen ukázka rozložení hlavní místnosti.
          </Typography>
          <List>
            {demoMessages.map((msg: PayloadMessage, index: number) => (
              <ListItem key={index} sx={{ p: 0, mb: 1 }}>
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'flex-start',
                    width: '100%',
                  }}
                >
                  <Box
                    sx={{
                      maxWidth: '70%',
                      p: 2,
                      borderRadius: 2,
                      backgroundColor: '#e0e0e0',
                      color: 'black',
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
          <TextField
            fullWidth
            label={pendingMessages.length > 0 ? `${pendingMessages.length} zpráv ve frontě - přihlas se pro odeslání` : "Napiš zprávu (bude ve frontě do přihlášení)"}
            variant="outlined"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={false}
          />
          <Button
            variant="contained"
            disabled={!message.trim()}
            onClick={sendMessage}
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

export default MainPagePreview;


