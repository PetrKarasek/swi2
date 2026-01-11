import React, { useMemo } from 'react';
import { Box, Typography, Paper, Badge } from '@mui/material';
import { PayloadMessage, UserToken } from '../types';
import AvatarComponent from './Avatar';

interface DMConversationsListProps {
  user: UserToken | null;
  conversations: { [username: string]: PayloadMessage[] };
  onSelectConversation: (username: string) => void;
  userAvatars: { [key: string]: string };
  selectedUser?: string;
  readConversations?: Set<string>;
  unreadPerUser?: {[key: string]: number};
}

const DEFAULT_AVATAR = "http://localhost:8081/avatars/cat.png";

const DMConversationsList = ({ 
  user, 
  conversations, 
  onSelectConversation, 
  userAvatars, 
  selectedUser,
  unreadPerUser = {} 
}: DMConversationsListProps) => {

  const getMessagePreview = (msg: PayloadMessage) => {
    if (!msg) return "";
    if (msg.fileUrl) {
        const isImage = msg.fileName?.match(/\.(jpg|jpeg|png|gif|webp)$/i) || msg.messageType === 'IMAGE';
        return isImage ? "📷 Image" : `📁 ${msg.fileName || 'File'}`;
    }
    if (msg.content && msg.content.startsWith("[FILE]")) {
        const match = msg.content.match(/^\s*\[FILE\]\s*(.*?)\s*\|\s*(.*)$/);
        if (match && match.length >= 2) {
            const fileName = match[1].trim();
            const isImage = fileName.match(/\.(jpg|jpeg|png|gif|webp)$/i);
            return isImage ? "📷 Image" : `📁 ${fileName}`;
        }
    }
    return msg.content;
  };

  const sortedConversations = useMemo(() => {
    if (!conversations) return [];

    return Object.entries(conversations)
      .filter(([, messages]) => Array.isArray(messages) && messages.length > 0)
      .map(([username, messages]) => {
        const lastMessage = messages[messages.length - 1];
        
        // Zde použijeme naši novou mapu. Pokud je chat otevřený, ukazujeme 0.
        const unreadCount = (selectedUser === username) ? 0 : (unreadPerUser[username] || 0);

        return {
          username,
          lastMessage,
          unreadCount
        };
      })
      .sort((a, b) => {
        const dateA = a.lastMessage?.date ? new Date(a.lastMessage.date).getTime() : 0;
        const dateB = b.lastMessage?.date ? new Date(b.lastMessage.date).getTime() : 0;
        return dateB - dateA;
      });
  }, [conversations, user?.username, selectedUser, unreadPerUser]);

  if (sortedConversations.length === 0) {
    return (
      <Box sx={{ p: 2, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          No direct messages yet
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2, overflowY: 'auto', maxHeight: '100%' }}>
      <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
        Direct Messages
      </Typography>
      
      {sortedConversations.map(({ username, lastMessage, unreadCount }) => (
        <Paper
          key={username}
          elevation={0}
          onClick={() => onSelectConversation(username)}
          sx={{
            p: 2,
            mb: 1.5,
            cursor: 'pointer',
            transition: 'all 0.2s',
            border: selectedUser === username ? '1px solid #1976d2' : '1px solid #e0e0e0',
            bgcolor: selectedUser === username ? 'rgba(25, 118, 210, 0.04)' : 'white',
            borderRadius: 2,
            '&:hover': {
              backgroundColor: '#f5f5f5',
              transform: 'translateX(4px)'
            }
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Badge color="error" badgeContent={unreadCount} invisible={unreadCount === 0}>
                <AvatarComponent
                avatarUrl={userAvatars[username] || DEFAULT_AVATAR}
                username={username}
                size={48}
                />
            </Badge>
            
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: unreadCount > 0 ? 700 : 500 }}>
                  {username}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {lastMessage?.date 
                    ? new Date(lastMessage.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                    : ''}
                </Typography>
              </Box>
              
              <Typography
                variant="body2"
                sx={{
                  color: unreadCount > 0 ? 'text.primary' : 'text.secondary',
                  fontWeight: unreadCount > 0 ? 600 : 400,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 0.5
                }}
              >
                {lastMessage && (
                    lastMessage.senderName === user?.username ? (
                    <span style={{opacity: 0.7}}>You: {getMessagePreview(lastMessage)}</span>
                    ) : (
                    <span>{getMessagePreview(lastMessage)}</span>
                    )
                )}
              </Typography>
            </Box>
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

export default DMConversationsList;