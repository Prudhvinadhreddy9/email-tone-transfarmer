import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './global.css';

// React 18 root API. StrictMode runs effects twice in dev to surface bugs;
// safe to keep in production builds (it's a no-op there).
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
