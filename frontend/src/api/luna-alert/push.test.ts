import { describe, expect, it, vi } from 'vitest';

import { createLunaAlertPushConnector, ALERTS_TOPIC } from './push';

const { createPushConnector } = vi.hoisted(() => ({ createPushConnector: vi.fn() }));

vi.mock('../shared', () => ({ createPushConnector }));

describe('createLunaAlertPushConnector', () => {
  it('that creating the connector should result into a connector for the topic of alerts', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaAlertPushConnector('http://localhost:7083', tokenSource);

    // then
    expect(createPushConnector).toHaveBeenCalledWith('http://localhost:7083', ALERTS_TOPIC, tokenSource);
  });
});
