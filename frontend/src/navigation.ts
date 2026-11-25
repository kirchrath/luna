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
}

export const QUICK_INPUT_ICON = SquarePen;

export const AREAS: readonly AreaDefinition[] = [
  { area: 'tasks', icon: SquareCheck },
  { area: 'calendar', icon: Calendar },
  { area: 'shopping', icon: ShoppingCart },
  { area: 'finance', icon: Wallet },
  { area: 'monitoring', icon: Activity },
];
