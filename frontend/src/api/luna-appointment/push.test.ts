import { describe, expect, it, vi } from 'vitest';

import { createLunaAppointmentPushConnector, APPOINTMENTS_TOPIC } from './push';

const { createPushConnector } = vi.hoisted(() => ({ createPushConnector: vi.fn() }));

vi.mock('../shared', () => ({ createPushConnector }));

describe('createLunaAppointmentPushConnector', () => {
  it('that creating the connector should result into a connector for the topic of appointments', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaAppointmentPushConnector('http://localhost:7085', tokenSource);

    // then
    expect(createPushConnector).toHaveBeenCalledWith('http://localhost:7085', APPOINTMENTS_TOPIC, tokenSource);
  });
});
