import { describe, expect, it, vi } from 'vitest';

import { createLunaAppointmentClient } from './client';

const { createAuthorizedClient } = vi.hoisted(() => ({ createAuthorizedClient: vi.fn(() => ({})) }));

vi.mock('../shared', () => ({ createAuthorizedClient }));

describe('createLunaAppointmentClient', () => {
  it('that creating the client should result into an authorized client for luna-appointment', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaAppointmentClient('http://localhost:7085', tokenSource);

    // then
    expect(createAuthorizedClient).toHaveBeenCalledWith('http://localhost:7085', tokenSource);
  });
});
