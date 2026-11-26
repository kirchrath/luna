import { afterEach, describe, expect, it, vi } from 'vitest';

import { createLunaCoreClient } from './client';

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

describe('createLunaCoreClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('that a signed in user should result into requests with a bearer token', async () => {
    // given
    const fetchMock = stubFetch();
    const client = createLunaCoreClient('http://localhost:7081', {
      getUser: () => Promise.resolve({ access_token: 'token-123' }),
    });

    // when
    await client.GET('/inbound-events/manual', { params: { query: { page: 0, size: 10 } } });

    // then
    const request = sentRequest(fetchMock);
    expect(request.url).toBe('http://localhost:7081/inbound-events/manual?page=0&size=10');
    expect(request.headers.get('Authorization')).toBe('Bearer token-123');
  });

  it('that a missing user should result into requests without authorization header', async () => {
    // given
    const fetchMock = stubFetch();
    const client = createLunaCoreClient('http://localhost:7081', { getUser: () => Promise.resolve(null) });

    // when
    await client.GET('/inbound-events/manual', {});

    // then
    expect(sentRequest(fetchMock).headers.has('Authorization')).toBe(false);
  });
});
