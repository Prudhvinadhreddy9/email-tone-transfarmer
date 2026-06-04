/// <reference types="vite/client" />

// Augment ImportMetaEnv with our app-specific Vite env vars so TypeScript
// knows the type of `import.meta.env.VITE_API_BASE_URL`.
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
