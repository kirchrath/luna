import type { components } from './schema';

type Schemas = components['schemas'];

export type AlertDto = Schemas['AlertDto'];
export type AlertDataDto = Schemas['AlertDataDto'];
export type AlertListDto = Schemas['AlertListDto'];
export type AlertListItemDto = Schemas['AlertListItemDto'];
export type AlertChangedDto = Schemas['AlertChangedDto'];
export type AlertStatusDto = AlertDataDto['status'];
export type AlertSeverityDto = AlertDataDto['severity'];
