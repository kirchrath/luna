import '@fontsource-variable/inter';
import '@fontsource-variable/space-grotesk';
import './styles/main.scss';

import { configure } from 'mobx';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthProvider } from 'react-oidc-context';

import { App } from './App';
import { AuthGate, createUserManager, onSigninCallback } from './auth';
import { initI18n } from './i18n';
import { createRootStore } from './stores';

const ROOT_ELEMENT_ID = 'root';

configure({ enforceActions: 'always' });

const root = document.getElementById(ROOT_ELEMENT_ID);
if (root === null) {
  throw new Error(`Element #${ROOT_ELEMENT_ID} not found`);
}

const userManager = createUserManager();
const rootStore = createRootStore(userManager);

void initI18n().then(() => {
  createRoot(root).render(
    <StrictMode>
      <AuthProvider userManager={userManager} onSigninCallback={onSigninCallback}>
        <AuthGate>
          <App rootStore={rootStore} />
        </AuthGate>
      </AuthProvider>
    </StrictMode>,
  );
});
