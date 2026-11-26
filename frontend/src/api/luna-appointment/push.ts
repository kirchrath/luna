import { createPushConnector, type PushConnector, type TokenSource } from '../shared';
import type { AppointmentChangedDto } from './types';

/** Topic of the push messages of luna-appointment (see luna-appointment/README.md). */
export const APPOINTMENTS_TOPIC = '/topic/appointments';

export type LunaAppointmentPushConnector = PushConnector<AppointmentChangedDto>;

export function createLunaAppointmentPushConnector(
  baseUrl: string,
  tokenSource: TokenSource,
): LunaAppointmentPushConnector {
  return createPushConnector<AppointmentChangedDto>(baseUrl, APPOINTMENTS_TOPIC, tokenSource);
}
