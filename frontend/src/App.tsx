import { useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';

import { routes } from './routes';
import { RootStore, StoresProvider } from './stores';

/** Created only after signing in, so that the router reads the address restored after the return from Keycloak. */
export function App() {
  const [router] = useState(() => createBrowserRouter(routes));
  const [rootStore] = useState(() => new RootStore());
  return (
    <StoresProvider rootStore={rootStore}>
      <RouterProvider router={router} />
    </StoresProvider>
  );
}
