import type { components } from './schema';

type Schemas = components['schemas'];

export type AppointmentDto = Schemas['AppointmentDto'];
export type AppointmentDataDto = Schemas['AppointmentDataDto'];
export type AppointmentListDto = Schemas['AppointmentListDto'];
export type AppointmentListItemDto = Schemas['AppointmentListItemDto'];
export type AppointmentUpdateDto = Schemas['AppointmentUpdateDto'];
export type AppointmentChangedDto = Schemas['AppointmentChangedDto'];
export type AppointmentStatusDto = AppointmentDataDto['status'];
export type AppointmentPeriodDto = 'UPCOMING' | 'PAST' | 'ALL';
