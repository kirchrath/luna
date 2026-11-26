import { Activity, Calendar, ShoppingCart, SquareCheck, SquarePen, Wallet, type LucideIcon } from 'lucide-react';

export const PATHS = {
  quickInput: '/',
  tasks: '/tasks',
  calendar: '/calendar',
  shopping: '/shopping',
  finance: '/finance',
  monitoring: '/monitoring',
} as const;

export type Area = Exclude<keyof typeof PATHS, 'quickInput'>;

export interface AreaDefinition {
  area: Area;
  icon: LucideIcon;
  /** Areas that do not exist yet are marked as "soon" and lead to a placeholder. */
  available: boolean;
}

export const TASK_ID_PARAMETER = 'id';
export const TASK_PATH = `${PATHS.tasks}/:${TASK_ID_PARAMETER}`;

export function taskPath(id: string): string {
  return `${PATHS.tasks}/${encodeURIComponent(id)}`;
}

export const QUICK_INPUT_ICON = SquarePen;

export const AREAS: readonly AreaDefinition[] = [
  { area: 'tasks', icon: SquareCheck, available: true },
  { area: 'calendar', icon: Calendar, available: false },
  { area: 'shopping', icon: ShoppingCart, available: false },
  { area: 'finance', icon: Wallet, available: false },
  { area: 'monitoring', icon: Activity, available: false },
];
