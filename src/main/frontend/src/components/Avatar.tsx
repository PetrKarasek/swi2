import React from 'react';
import { Box, Avatar as MuiAvatar, Typography } from '@mui/material';

interface AvatarProps {
  avatarUrl?: string;
  username?: string;
  size?: number;
  onClick?: () => void;
  clickable?: boolean;
}

const Avatar: React.FC<AvatarProps> = ({ 
  avatarUrl, 
  username, 
  size = 40, 
  onClick, 
  clickable = false 
}) => {
  const getInitials = (name?: string) => {
    if (!name) return '?';
    return name.charAt(0).toUpperCase();
  };

  return (
    <Box
      sx={{
        position: 'relative',
        cursor: clickable ? 'pointer' : 'default',
        '&:hover': clickable ? {
          '&::after': {
            content: '""',
            position: 'absolute',
            inset: -2,
            borderRadius: '50%',
            border: '2px solid #1976d2',
          }
        } : {}
      }}
      onClick={onClick}
    >
      <MuiAvatar
        src={avatarUrl}
        sx={{
          width: size,
          height: size,
          bgcolor: avatarUrl ? 'transparent' : '#1976d2',
          fontSize: size * 0.6,
          fontWeight: 'bold',
        }}
      >
        {!avatarUrl && getInitials(username)}
      </MuiAvatar>
    </Box>
  );
};

export default Avatar;
