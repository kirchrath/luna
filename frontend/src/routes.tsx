import type { RouteObject } from 'react-router';

import { AppLayout } from './layout/AppLayout';
import { AREAS, PATHS } from './navigation';
import { ComingSoonPage } from './pages/ComingSoonPage';
import { QuickInputPage } from './pages/QuickInputPage';

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
