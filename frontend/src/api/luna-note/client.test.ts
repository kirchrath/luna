import { describe, expect, it, vi } from 'vitest';

import { createLunaNoteClient } from './client';

const { createAuthorizedClient } = vi.hoisted(() => ({ createAuthorizedClient: vi.fn(() => ({})) }));

vi.mock('../shared', () => ({ createAuthorizedClient }));

describe('createLunaNoteClient', () => {
  it('that creating the client should result into an authorized client for luna-note', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaNoteClient('http://localhost:7084', tokenSource);

    // then
    expect(createAuthorizedClient).toHaveBeenCalledWith('http://localhost:7084', tokenSource);
  });
});
