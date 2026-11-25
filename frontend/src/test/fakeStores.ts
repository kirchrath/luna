import { vi } from 'vitest';

import type { LunaCoreClient, ManualInboundEventListDto, PushConnector, PushHandlers } from '../api';
import { RootStore } from '../stores';

const EMPTY_LIST: ManualInboundEventListDto = { items: [], totalPages: 0, totalCount: 0 };

export function fakeLunaCoreClient(list: ManualInboundEventListDto = EMPTY_LIST) {
  const GET = vi.fn(() => Promise.resolve({ data: list, error: undefined, response: new Response() }));
  const POST = vi.fn(() =>
    Promise.resolve({ data: { id: 'new-id' }, error: undefined, response: new Response(null, { status: 202 }) }),
  );
  return { GET, POST, client: { GET, POST } as unknown as LunaCoreClient };
}

export function fakePushConnector() {
  const handlers: PushHandlers[] = [];
  const close = vi.fn(() => Promise.resolve());
  const connect = vi.fn<PushConnector>((pushHandlers) => {
    handlers.push(pushHandlers);
    return { close };
  });
  return { connect, close, handlers };
}

export function fakeRootStore(list?: ManualInboundEventListDto) {
  const lunaCore = fakeLunaCoreClient(list);
  const push = fakePushConnector();
  return {
    rootStore: new RootStore({ lunaCoreClient: lunaCore.client, connectLunaCorePush: push.connect }),
    lunaCore,
    push,
  };
}
