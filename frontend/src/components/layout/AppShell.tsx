import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import Toolbar from '@mui/material/Toolbar';
import { Outlet } from 'react-router';

import { AdSlot } from '@/features/ads/AdSlot';
import { GuidedTour } from '@/features/tour/GuidedTour';
import { testIds } from '@/lib/testids';
import { useUiStore } from '@/store/uiStore';

import { Header } from './Header';
import { Sidebar } from './Sidebar';

const SIDEBAR_WIDTH = 248;

/**
 * Estructura base de la aplicacion.
 *
 * En escritorio la navegacion queda fija; en movil se abre como panel temporal.
 * Todas las pantallas viven dentro de este shell.
 */
export function AppShell() {
  const sidebarOpen = useUiStore((state) => state.sidebarOpen);
  const setSidebarOpen = useUiStore((state) => state.setSidebarOpen);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }} data-testid={testIds.layout.appShell}>
      <Header onOpenSidebar={() => setSidebarOpen(true)} sidebarWidth={SIDEBAR_WIDTH} />

      <Box component="nav" sx={{ width: { md: SIDEBAR_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: SIDEBAR_WIDTH, boxSizing: 'border-box' },
          }}
        >
          <Sidebar onNavigate={() => setSidebarOpen(false)} />
        </Drawer>

        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: SIDEBAR_WIDTH, boxSizing: 'border-box' },
          }}
        >
          <Sidebar />
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          px: { xs: 4, md: 8 },
          pb: 12,
        }}
      >
        <Toolbar sx={{ minHeight: 64 }} />
        <Outlet />

        {/* Un espacio por pantalla: es como LUMA se sostiene sin cobrar por
            el uso. No vive dentro de cada pagina porque es el mismo anuncio
            en el mismo lugar para las seis, y aqui es donde el shell ya sabe
            que esta a punto de terminar el contenido. */}
        <AdSlot />
      </Box>

      <GuidedTour />
    </Box>
  );
}
