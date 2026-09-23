import { RouterProvider } from 'react-router';

import { SessionGate } from '@/features/auth/SessionGate';
import { router } from '@/routes/router';

import { Providers } from './Providers';

export function App() {
  return (
    <Providers>
      <SessionGate>
        <RouterProvider router={router} />
      </SessionGate>
    </Providers>
  );
}
