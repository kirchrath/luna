import { describe, expect, it, vi } from 'vitest';

import { createLunaNotePushConnector, NOTES_TOPIC } from './push';

const { createPushConnector } = vi.hoisted(() => ({ createPushConnector: vi.fn() }));

vi.mock('../shared', () => ({ createPushConnector }));

describe('createLunaNotePushConnector', () => {
  it('that creating the connector should result into a connector for the topic of notes', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    createLunaNotePushConnector('http://localhost:7084', tokenSource);

    // then
    expect(createPushConnector).toHaveBeenCalledWith('http://localhost:7084', NOTES_TOPIC, tokenSource);
  });
});
