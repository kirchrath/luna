import type { RouteObject } from 'react-router';

import { AppLayout } from './layout';
import { ALERT_PATH, AREAS, NOTE_PATH, PATHS, TASK_PATH } from './navigation';
import {
  AlertDetailPage,
  AlertListPage,
  ComingSoonPage,
  NoteDetailPage,
  NoteListPage,
  QuickInputPage,
  TaskDetailPage,
  TaskListPage,
} from './pages';

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: PATHS.quickInput, element: <QuickInputPage /> },
      { path: PATHS.tasks, element: <TaskListPage /> },
      { path: TASK_PATH, element: <TaskDetailPage /> },
      { path: PATHS.notes, element: <NoteListPage /> },
      { path: NOTE_PATH, element: <NoteDetailPage /> },
      { path: PATHS.alerts, element: <AlertListPage /> },
      { path: ALERT_PATH, element: <AlertDetailPage /> },
      ...AREAS.filter(({ available }) => !available).map((definition) => ({
        path: PATHS[definition.area],
        element: <ComingSoonPage {...definition} />,
      })),
    ],
  },
];
