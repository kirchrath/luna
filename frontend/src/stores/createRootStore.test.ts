import { describe, expect, it } from 'vitest';

import { createRootStore } from './createRootStore';
import { InboundEventStore } from './InboundEventStore';

describe('createRootStore', () => {
  it('that creating the root store should result into all stores', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    const rootStore = createRootStore(tokenSource, { VITE_LUNA_CORE_URL: 'http://localhost:7081' } as ImportMetaEnv);

    // then
    expect(rootStore.inboundEventStore).toBeInstanceOf(InboundEventStore);
  });

  it('that creating the root store without environment should result into the vite environment', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    const rootStore = createRootStore(tokenSource);

    // then
    expect(rootStore.inboundEventStore).toBeInstanceOf(InboundEventStore);
  });
});
