import type { Client } from 'openapi-fetch';

import { createAuthorizedClient, type TokenSource } from '../shared';
import type { paths } from './schema';

export type LunaTaskClient = Client<paths>;

export function createLunaTaskClient(baseUrl: string, tokenSource: TokenSource): LunaTaskClient {
  return createAuthorizedClient<paths>(baseUrl, tokenSource);
}
