import { describe, expect, it, vi } from 'vitest';

import { createLunaCorePushConnector, INBOUND_EVENTS_TOPIC } from './push';

const { createPushConnector } = vi.hoisted(() => ({ createPushConnector: vi.fn() }));

vi.mock('../shared', () => ({ createPushConnector }));

describe('createLunaCorePushConnector', () => {
  it('that creating the connector should result into a connector for the topic of inbound events', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaCorePushConnector('http://localhost:7081', tokenSource);

    // then
    expect(createPushConnector).toHaveBeenCalledWith('http://localhost:7081', INBOUND_EVENTS_TOPIC, tokenSource);
  });
});
