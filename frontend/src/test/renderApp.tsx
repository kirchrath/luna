import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';

import { initI18n } from '../i18n';
import { routes } from '../routes';

export async function renderApp(path: string) {
  await initI18n();
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  return { router, ...render(<RouterProvider router={router} />) };
}
