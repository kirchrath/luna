import { describe, expect, it, vi } from 'vitest';

import { createLunaCoreClient } from './client';

const { createAuthorizedClient } = vi.hoisted(() => ({ createAuthorizedClient: vi.fn(() => ({})) }));

vi.mock('../shared', () => ({ createAuthorizedClient }));

describe('createLunaCoreClient', () => {
  it('that creating the client should result into an authorized client for luna-core', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaCoreClient('http://localhost:7081', tokenSource);

    // then
    expect(createAuthorizedClient).toHaveBeenCalledWith('http://localhost:7081', tokenSource);
  });
});
