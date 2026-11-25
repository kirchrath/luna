import {
  CalendarDays,
  FileText,
  Receipt,
  Sparkles,
  SquareCheck,
  StickyNote,
  TriangleAlert,
  type LucideIcon,
} from 'lucide-react';

import type { LunaElementTypeDto } from '../api';

export const AUTOMATIC = 'AUTOMATIC';
export type TypeChoice = LunaElementTypeDto | typeof AUTOMATIC;
export const AUTOMATIC_ICON = Sparkles;

export const ELEMENT_TYPE_ICONS: Record<LunaElementTypeDto, LucideIcon> = {
  TASK: SquareCheck,
  APPOINTMENT: CalendarDays,
  INVOICE: Receipt,
  CONTRACT: FileText,
  ALERT: TriangleAlert,
  NOTE: StickyNote,
};

/** Order of the type chips, as in the wireframes. */
export const ELEMENT_TYPE_ORDER: readonly LunaElementTypeDto[] = [
  'TASK',
  'APPOINTMENT',
  'INVOICE',
  'CONTRACT',
  'ALERT',
  'NOTE',
];
