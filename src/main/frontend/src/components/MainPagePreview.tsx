import { Box, Button, TextField, Typography, Paper, List, ListItem, AppBar, Toolbar } from "@mui/material";
import React from "react";
import { PayloadMessage } from "../types";

// Simple static preview of the main room for users who are not logged in.
// They can see the layout but not the real messages.

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

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2, backgroundColor: '#f3f4f6' }}>
        <Paper
          sx={{
            flex: 1,
            p: 2,
            mb: 2,
            overflow: 'auto',
            backgroundColor: '#ffffff',
          }}
        >
          <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>
            Z důvodu zadání mohou zprávy číst pouze přihlášení uživatelé.
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

        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField
            fullWidth
            label="Přihlas se, aby bylo možné odesílat zprávy"
            variant="outlined"
            disabled
          />
          <Button
            variant="contained"
            disabled
            sx={{ minWidth: 80 }}
          >
            Send
          </Button>
        </Box>
      </Box>
    </Box>
  );
};

export default MainPagePreview;


