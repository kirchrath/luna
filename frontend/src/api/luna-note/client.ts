import type { Client } from 'openapi-fetch';

import { createAuthorizedClient, type TokenSource } from '../shared';
import type { paths } from './schema';

export type LunaNoteClient = Client<paths>;

export function createLunaNoteClient(baseUrl: string, tokenSource: TokenSource): LunaNoteClient {
  return createAuthorizedClient<paths>(baseUrl, tokenSource);
}
