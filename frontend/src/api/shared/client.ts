import createClient, { type Client } from 'openapi-fetch';

import type { TokenSource } from './tokenSource';

const AUTHORIZATION = 'Authorization';
const BEARER = 'Bearer';

/** Creates an openapi-fetch client that sends the access token of the signed in user with every request. */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type -- the constraint of openapi-fetch's Client
export function createAuthorizedClient<Paths extends {}>(baseUrl: string, tokenSource: TokenSource): Client<Paths> {
  const client = createClient<Paths>({ baseUrl });
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
