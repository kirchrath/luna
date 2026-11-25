import { render } from '@testing-library/react';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import { createMemoryRouter, RouterProvider } from 'react-router';

import { initI18n } from '../i18n';
import { routes } from '../routes';
import { RootStore, StoresProvider } from '../stores';
import { fakeAuth } from './fakeAuth';

interface RenderAppOptions {
  rootStore?: RootStore;
  auth?: AuthContextProps;
}

export async function renderApp(
  path: string,
  { rootStore = new RootStore(), auth = fakeAuth() }: RenderAppOptions = {},
) {
  await initI18n();
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  return {
    router,
    ...render(
      <AuthContext value={auth}>
        <StoresProvider rootStore={rootStore}>
          <RouterProvider router={router} />
        </StoresProvider>
      </AuthContext>,
    ),
  };
}
