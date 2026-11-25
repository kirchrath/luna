import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';

import { initI18n } from '../i18n';
import { routes } from '../routes';
import { RootStore, StoresProvider } from '../stores';

export async function renderApp(path: string, rootStore = new RootStore()) {
  await initI18n();
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  return {
    router,
    ...render(
      <StoresProvider rootStore={rootStore}>
        <RouterProvider router={router} />
      </StoresProvider>,
    ),
  };
}
