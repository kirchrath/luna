import { describe, expect, it, vi } from 'vitest';

import { createLunaTaskClient } from './client';

const { createAuthorizedClient } = vi.hoisted(() => ({ createAuthorizedClient: vi.fn(() => ({})) }));

vi.mock('../shared', () => ({ createAuthorizedClient }));

describe('createLunaTaskClient', () => {
  it('that creating the client should result into an authorized client for luna-task', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaTaskClient('http://localhost:7082', tokenSource);

    // then
    expect(createAuthorizedClient).toHaveBeenCalledWith('http://localhost:7082', tokenSource);
  });
});
