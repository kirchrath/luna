import type { RouteObject } from 'react-router';

import { AppLayout } from './layout';
import { AREAS, PATHS, TASK_PATH } from './navigation';
import { ComingSoonPage, QuickInputPage, TaskDetailPage, TaskListPage } from './pages';

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: PATHS.quickInput, element: <QuickInputPage /> },
      { path: PATHS.tasks, element: <TaskListPage /> },
      { path: TASK_PATH, element: <TaskDetailPage /> },
      ...AREAS.filter(({ available }) => !available).map((definition) => ({
        path: PATHS[definition.area],
        element: <ComingSoonPage {...definition} />,
      })),
    ],
  },
];
