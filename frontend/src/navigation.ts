import {
  Activity,
  Calendar,
  ShoppingCart,
  SquareCheck,
  SquarePen,
  StickyNote,
  TriangleAlert,
  Wallet,
  type LucideIcon,
} from 'lucide-react';

export const PATHS = {
  quickInput: '/',
  tasks: '/tasks',
  notes: '/notes',
  calendar: '/calendar',
  shopping: '/shopping',
  finance: '/finance',
  monitoring: '/monitoring',
  alerts: '/alerts',
} as const;

export type Area = Exclude<keyof typeof PATHS, 'quickInput'>;

export interface AreaDefinition {
  area: Area;
  icon: LucideIcon;
  /** Areas that do not exist yet are marked as "soon" and lead to a placeholder. */
  available: boolean;
}

/** Detail pages of the elements: `<area>/:id`. */
export const ID_PARAMETER = 'id';
export const TASK_PATH = `${PATHS.tasks}/:${ID_PARAMETER}`;
export const NOTE_PATH = `${PATHS.notes}/:${ID_PARAMETER}`;
export const ALERT_PATH = `${PATHS.alerts}/:${ID_PARAMETER}`;

function detailPath(area: string, id: string): string {
  return `${area}/${encodeURIComponent(id)}`;
}

export function taskPath(id: string): string {
  return detailPath(PATHS.tasks, id);
}

export function notePath(id: string): string {
  return detailPath(PATHS.notes, id);
}

export function alertPath(id: string): string {
  return detailPath(PATHS.alerts, id);
}

export const QUICK_INPUT_ICON = SquarePen;

export const AREAS: readonly AreaDefinition[] = [
  { area: 'tasks', icon: SquareCheck, available: true },
  { area: 'notes', icon: StickyNote, available: true },
  { area: 'calendar', icon: Calendar, available: false },
  { area: 'shopping', icon: ShoppingCart, available: false },
  { area: 'finance', icon: Wallet, available: false },
  { area: 'monitoring', icon: Activity, available: false },
  { area: 'alerts', icon: TriangleAlert, available: true },
];
