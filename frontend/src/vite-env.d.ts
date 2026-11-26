/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_OIDC_AUTHORITY: string;
  readonly VITE_OIDC_CLIENT_ID: string;
  readonly VITE_LUNA_CORE_URL: string;
  readonly VITE_LUNA_TASK_URL: string;
  readonly VITE_LUNA_ALERT_URL: string;
  readonly VITE_LUNA_NOTE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
