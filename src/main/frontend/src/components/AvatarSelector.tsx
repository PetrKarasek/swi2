import React, { useState, useEffect } from 'react';
import {
  Box,
  Dialog,
  DialogTitle,
  DialogContent,
  Avatar as MuiAvatar,
  Button,
  Typography
} from '@mui/material';
import axios from 'axios';

interface AvatarSelectorProps {
  open: boolean;
  onClose: () => void;
  username: string;
  currentAvatar?: string;
  onAvatarChange: (avatarUrl: string) => void;
}

const AvatarSelector: React.FC<AvatarSelectorProps> = ({
                                                         open,
                                                         onClose,
                                                         username,
                                                         currentAvatar,
                                                         onAvatarChange
                                                       }) => {
  const [availableAvatars, setAvailableAvatars] = useState<string[]>([]);
  const [selectedAvatar, setSelectedAvatar] = useState<string>(currentAvatar || '');

  useEffect(() => {
    loadAvatars();
  }, []);

  useEffect(() => {
    setSelectedAvatar(currentAvatar || '');
  }, [currentAvatar]);

  const loadAvatars = async () => {
    try {
      const result = await axios.get<string[]>('http://localhost:8081/api/avatars');
      if (result.data) {
        setAvailableAvatars(result.data);
      }
    } catch (error) {
      console.error('Error loading avatars:', error);
      // Fallback avatars if API fails
      setAvailableAvatars([
        '/avatars/cat.png',
        '/avatars/female.png',
        '/avatars/kapuce.png',
        '/avatars/male.png',
        '/avatars/robot.png'
      ]);
    }
  };

  const resolveAvatarUrl = (url: string) => {
    if (!url) return '';
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:')) return url;
    if (url.startsWith('/')) return `http://localhost:8081${url}`;
    return `http://localhost:8081/avatars/${url}`;
  };

  const handleAvatarSelect = (avatarUrl: string) => {
    setSelectedAvatar(avatarUrl);
  };

  const handleConfirm = async () => {
    try {
      const avatarIndex = availableAvatars.indexOf(selectedAvatar);
      console.log('Confirming avatar:', selectedAvatar, 'index:', avatarIndex, 'username:', username);

      if (avatarIndex !== -1) {
        const response = await axios.post('http://localhost:8081/api/select-avatar', null, {
          params: {
            username: username,
            avatarIndex: avatarIndex
          }
        });
        console.log('Avatar selection response:', response);
        onAvatarChange(selectedAvatar);
        onClose();
      }
    } catch (error) {
      console.error('Error selecting avatar:', error);
      // Still close the dialog and update locally even if API fails
      onAvatarChange(selectedAvatar);
      onClose();
    }
  };

  return (
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Choose Your Avatar</DialogTitle>
        <DialogContent>
          <Box sx={{ p: 2 }}>
            <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
              Click on an avatar to select it, then confirm your choice.
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, justifyContent: 'center', mb: 3 }}>
              {availableAvatars.map((avatarUrl, index) => (
                  <Box
                      key={index}
                      sx={{
                        position: 'relative',
                        cursor: 'pointer',
                        '&:hover': {
                          transform: 'scale(1.1)',
                        },
                        transition: 'transform 0.2s',
                      }}
                      onClick={() => handleAvatarSelect(avatarUrl)}
                  >
                    <MuiAvatar
                        src={resolveAvatarUrl(avatarUrl)}
                        sx={{
                          width: 80,
                          height: 80,
                          border: selectedAvatar === avatarUrl ? '3px solid #1976d2' : '2px solid #e0e0e0',
                          boxSizing: 'border-box',
                        }}
                    />
                    {selectedAvatar === avatarUrl && (
                        <Box
                            sx={{
                              position: 'absolute',
                              top: -5,
                              right: -5,
                              bgcolor: '#1976d2',
                              color: 'white',
                              borderRadius: '50%',
                              width: 24,
                              height: 24,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontSize: 12,
                            }}
                        >
                          ✓
                        </Box>
                    )}
                  </Box>
              ))}
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3, gap: 2 }}>
              <Button onClick={onClose}>Cancel</Button>
              <Button
                  variant="contained"
                  onClick={handleConfirm}
                  disabled={!selectedAvatar}
              >
                Confirm
              </Button>
            </Box>
          </Box>
        </DialogContent>
      </Dialog>
  );
};

export default AvatarSelector;
