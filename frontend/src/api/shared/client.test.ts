import { afterEach, describe, expect, it, vi } from 'vitest';

import { createAuthorizedClient } from './client';

/** Minimal API in the shape openapi-typescript generates. */
interface Paths {
  '/things': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never };
    get: {
      parameters: { query?: { page?: number; size?: number }; header?: never; path?: never; cookie?: never };
      requestBody?: never;
      responses: { 200: { headers: Record<string, unknown>; content: { 'application/json': unknown } } };
    };
    put?: never;
    post?: never;
    delete?: never;
    options?: never;
    head?: never;
    patch?: never;
    trace?: never;
  };
}

function stubFetch() {
  const fetchMock = vi.fn<(request: Request) => Promise<Response>>(() =>
    Promise.resolve(
      new Response(JSON.stringify({ items: [], totalPages: 0, totalCount: 0 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function sentRequest(fetchMock: ReturnType<typeof stubFetch>): Request {
  const [call] = fetchMock.mock.calls;
  if (call === undefined) {
    throw new Error('no request sent');
  }
  return call[0];
}

describe('createAuthorizedClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('that a signed in user should result into requests with a bearer token', async () => {
    // given
    const fetchMock = stubFetch();
    const client = createAuthorizedClient<Paths>('http://localhost:7081', {
      getUser: () => Promise.resolve({ access_token: 'token-123' }),
    });

    // when
    await client.GET('/things', { params: { query: { page: 0, size: 10 } } });

    // then
    const request = sentRequest(fetchMock);
    expect(request.url).toBe('http://localhost:7081/things?page=0&size=10');
    expect(request.headers.get('Authorization')).toBe('Bearer token-123');
  });

  it('that a missing user should result into requests without authorization header', async () => {
    // given
    const fetchMock = stubFetch();
    const client = createAuthorizedClient<Paths>('http://localhost:7081', { getUser: () => Promise.resolve(null) });

    // when
    await client.GET('/things', {});

    // then
    expect(sentRequest(fetchMock).headers.has('Authorization')).toBe(false);
  });
});
