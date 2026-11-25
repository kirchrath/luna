import { useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';

import { routes } from './routes';
import { StoresProvider, type RootStore } from './stores';

interface AppProps {
  rootStore: RootStore;
}

/** Rendered only after signing in, so that the router reads the address restored after the return from Keycloak. */
export function App({ rootStore }: AppProps) {
  const [router] = useState(() => createBrowserRouter(routes));
  return (
    <StoresProvider rootStore={rootStore}>
      <RouterProvider router={router} />
    </StoresProvider>
  );
}
