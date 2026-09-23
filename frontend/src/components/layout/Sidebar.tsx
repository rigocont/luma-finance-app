import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useLocation, useNavigate } from 'react-router';

import { testIds } from '@/lib/testids';

import { LumaMark } from './LumaMark';
import { navItems } from './navigation';

interface SidebarProps {
  onNavigate?: () => void;
}

export function Sidebar({ onNavigate }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <Box
      data-testid={testIds.layout.sidebar}
      sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
    >
      <Stack
        direction="row"
        spacing={2.5}
        sx={{ alignItems: 'center', px: 5, height: 64, flex: 'none' }}
      >
        <LumaMark />
        <Typography
          component="span"
          sx={{ fontWeight: 800, letterSpacing: '0.14em', fontSize: '1.0625rem' }}
        >
          LUMA
        </Typography>
      </Stack>

      <List component="nav" sx={{ pt: 2 }}>
        {navItems.map((item) => {
          const selected =
            item.path === '/' ? location.pathname === '/' : location.pathname.startsWith(item.path);
          const Icon = item.icon;

          return (
            <ListItemButton
              key={item.key}
              data-testid={testIds.layout.navItem(item.key)}
              selected={selected}
              onClick={() => {
                navigate(item.path);
                onNavigate?.();
              }}
            >
              <ListItemIcon>
                <Icon fontSize="small" />
              </ListItemIcon>
              <ListItemText
                disableTypography
                primary={
                  <Typography
                    component="span"
                    sx={{ fontSize: '0.875rem', fontWeight: selected ? 600 : 500 }}
                  >
                    {item.label}
                  </Typography>
                }
              />
            </ListItemButton>
          );
        })}
      </List>

      <Box sx={{ flex: 1 }} />

      <Typography variant="caption" color="text.disabled" sx={{ px: 6, pb: 5 }}>
        Version 0.1.0 &middot; Fase 1
      </Typography>
    </Box>
  );
}
