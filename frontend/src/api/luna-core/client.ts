import type { Client } from 'openapi-fetch';

import { createAuthorizedClient, type TokenSource } from '../shared';
import type { paths } from './schema';

export type LunaCoreClient = Client<paths>;

export function createLunaCoreClient(baseUrl: string, tokenSource: TokenSource): LunaCoreClient {
  return createAuthorizedClient<paths>(baseUrl, tokenSource);
}
