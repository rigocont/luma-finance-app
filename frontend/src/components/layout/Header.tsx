import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import MenuIcon from '@mui/icons-material/Menu';
import AppBar from '@mui/material/AppBar';
import IconButton from '@mui/material/IconButton';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';

import { useCurrentUser, useLogout } from '@/features/auth/useAuth';
import { NotificationBell } from '@/features/notifications/NotificationBell';
import { testIds } from '@/lib/testids';
import { useUiStore } from '@/store/uiStore';

interface HeaderProps {
  onOpenSidebar: () => void;
  sidebarWidth: number;
}

export function Header({ onOpenSidebar, sidebarWidth }: HeaderProps) {
  const theme = useTheme();
  const setThemePreference = useUiStore((state) => state.setThemePreference);
  const isDark = theme.palette.mode === 'dark';
  const user = useCurrentUser();
  const logout = useLogout();

  return (
    <AppBar
      position="fixed"
      data-testid={testIds.layout.header}
      sx={{
        width: { md: `calc(100% - ${sidebarWidth}px)` },
        ml: { md: `${sidebarWidth}px` },
      }}
    >
      <Toolbar sx={{ gap: 2, minHeight: 64 }}>
        <IconButton
          edge="start"
          aria-label="Abrir menu"
          data-testid={testIds.layout.sidebarToggle}
          onClick={onOpenSidebar}
          sx={{ display: { md: 'none' } }}
        >
          <MenuIcon />
        </IconButton>

        <div style={{ flex: 1 }} />

        {user && (
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid={testIds.auth.userName}
            sx={{ display: { xs: 'none', sm: 'block' } }}
          >
            {user.name}
          </Typography>
        )}

        <NotificationBell />

        <Tooltip title={isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}>
          <IconButton
            aria-label="Cambiar tema"
            data-testid={testIds.layout.themeToggle}
            onClick={() => setThemePreference(isDark ? 'light' : 'dark')}
          >
            {isDark ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
          </IconButton>
        </Tooltip>

        <Tooltip title="Cerrar sesion">
          <IconButton
            aria-label="Cerrar sesion"
            data-testid={testIds.auth.logoutButton}
            onClick={() => void logout()}
          >
            <LogoutOutlinedIcon />
          </IconButton>
        </Tooltip>
      </Toolbar>
    </AppBar>
  );
}
