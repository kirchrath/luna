import '@fontsource-variable/inter';
import '@fontsource-variable/space-grotesk';
import './styles/main.scss';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';

import { initI18n } from './i18n';
import { routes } from './routes';

const ROOT_ELEMENT_ID = 'root';

const root = document.getElementById(ROOT_ELEMENT_ID);
if (root === null) {
  throw new Error(`Element #${ROOT_ELEMENT_ID} not found`);
}

void initI18n().then(() => {
  createRoot(root).render(
    <StrictMode>
      <RouterProvider router={createBrowserRouter(routes)} />
    </StrictMode>,
  );
});
