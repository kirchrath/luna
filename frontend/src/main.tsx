import '@fontsource-variable/inter';
import '@fontsource-variable/space-grotesk';
import './styles/main.scss';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthProvider } from 'react-oidc-context';

import { App } from './App';
import { AuthGate, createOidcConfig } from './auth';
import { initI18n } from './i18n';

const ROOT_ELEMENT_ID = 'root';

const root = document.getElementById(ROOT_ELEMENT_ID);
if (root === null) {
  throw new Error(`Element #${ROOT_ELEMENT_ID} not found`);
}

void initI18n().then(() => {
  createRoot(root).render(
    <StrictMode>
      <AuthProvider {...createOidcConfig()}>
        <AuthGate>
          <App />
        </AuthGate>
      </AuthProvider>
    </StrictMode>,
  );
});
