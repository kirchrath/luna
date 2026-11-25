import createClient, { type Client } from 'openapi-fetch';

import type { paths } from './schema';
import type { TokenSource } from './tokenSource';

const AUTHORIZATION = 'Authorization';
const BEARER = 'Bearer';

export type LunaCoreClient = Client<paths>;

export function createLunaCoreClient(baseUrl: string, tokenSource: TokenSource): LunaCoreClient {
  const client = createClient<paths>({ baseUrl });
  client.use({
    async onRequest({ request }) {
      const user = await tokenSource.getUser();
      if (user !== null) {
        request.headers.set(AUTHORIZATION, `${BEARER} ${user.access_token}`);
      }
      return request;
    },
  });
  return client;
}
