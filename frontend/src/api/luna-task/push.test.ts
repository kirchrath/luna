import { describe, expect, it, vi } from 'vitest';

import { createLunaTaskPushConnector, TASKS_TOPIC } from './push';

const { createPushConnector } = vi.hoisted(() => ({ createPushConnector: vi.fn() }));

vi.mock('../shared', () => ({ createPushConnector }));

describe('createLunaTaskPushConnector', () => {
  it('that creating the connector should result into a connector for the topic of tasks', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaTaskPushConnector('http://localhost:7082', tokenSource);

    // then
    expect(createPushConnector).toHaveBeenCalledWith('http://localhost:7082', TASKS_TOPIC, tokenSource);
  });
});
