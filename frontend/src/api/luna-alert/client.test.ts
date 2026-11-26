import { describe, expect, it, vi } from 'vitest';

import { createLunaAlertClient } from './client';

const { createAuthorizedClient } = vi.hoisted(() => ({ createAuthorizedClient: vi.fn(() => ({})) }));

vi.mock('../shared', () => ({ createAuthorizedClient }));

describe('createLunaAlertClient', () => {
  it('that creating the client should result into an authorized client for luna-alert', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaAlertClient('http://localhost:7083', tokenSource);

    // then
    expect(createAuthorizedClient).toHaveBeenCalledWith('http://localhost:7083', tokenSource);
  });
});
