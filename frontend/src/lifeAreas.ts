import { Briefcase, House, Layers, type LucideIcon } from 'lucide-react';

import type { LifeAreaDto } from './api';

export const LIFE_AREA_ICONS: Record<LifeAreaDto, LucideIcon> = {
  PERSONAL: House,
  WORK: Briefcase,
};

/** Shown for the choice of all life areas. */
export const ALL_LIFE_AREAS_ICON: LucideIcon = Layers;
