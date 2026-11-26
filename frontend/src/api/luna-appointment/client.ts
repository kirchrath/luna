import type { Client } from 'openapi-fetch';

import { createAuthorizedClient, type TokenSource } from '../shared';
import type { paths } from './schema';

export type LunaAppointmentClient = Client<paths>;

export function createLunaAppointmentClient(baseUrl: string, tokenSource: TokenSource): LunaAppointmentClient {
  return createAuthorizedClient<paths>(baseUrl, tokenSource);
}
