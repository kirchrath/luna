import type { Client } from 'openapi-fetch';

import { createAuthorizedClient, type TokenSource } from '../shared';
import type { paths } from './schema';

export type LunaAlertClient = Client<paths>;

export function createLunaAlertClient(baseUrl: string, tokenSource: TokenSource): LunaAlertClient {
  return createAuthorizedClient<paths>(baseUrl, tokenSource);
}
