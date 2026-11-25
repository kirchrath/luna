import type { RouteObject } from 'react-router';

import { AppLayout } from './layout';
import { AREAS, PATHS } from './navigation';
import { ComingSoonPage, QuickInputPage } from './pages';

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: PATHS.quickInput, element: <QuickInputPage /> },
      ...AREAS.map((definition) => ({
        path: PATHS[definition.area],
        element: <ComingSoonPage {...definition} />,
      })),
    ],
  },
];
