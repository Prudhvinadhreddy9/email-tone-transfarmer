import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// WHY this config:
//  - server.host=true makes Vite listen on 0.0.0.0, which is required for
//    Docker port mapping. On a bare local dev box this is harmless.
//  - server.port pinned to 5173 so the backend's CORS allow-list is stable.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
  },
});
