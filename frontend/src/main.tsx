import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import '@/i18n';

import { App } from './app/App';

const container = document.getElementById('root');

if (!container) {
  throw new Error('No se encontro el elemento #root');
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
