import { describe, expect, it, vi } from 'vitest';

import type { LunaCoreClient, InboundEventListItemDto } from '../api';
import { fakeLunaCoreClient, fakePushConnector } from '../test';
import { LifeAreaStore } from './LifeAreaStore';
import { InboundEventStore } from './InboundEventStore';

function item(id: string, status: InboundEventListItemDto['status'] = 'RECEIVED'): InboundEventListItemDto {
  return {
    area: 'PERSONAL',
    id,
    source: 'MANUAL',
    text: `Input ${id}`,
    status,
    createdDate: '2026-11-25T10:00:00+01:00',
    elements: [],
  };
}

function items(count: number, prefix = 'item'): InboundEventListItemDto[] {
  return Array.from({ length: count }, (_, index) => item(`${prefix}-${String(index)}`));
}

function createStore(lunaCore = fakeLunaCoreClient(), push = fakePushConnector(), lifeAreaStore = new LifeAreaStore()) {
  return {
    store: new InboundEventStore(lunaCore.client, push.connect, lifeAreaStore),
    lunaCore,
    push,
    lifeAreaStore,
  };
}

function respondWith(lunaCore: ReturnType<typeof fakeLunaCoreClient>, list: InboundEventListItemDto[], total: number) {
  lunaCore.GET.mockResolvedValueOnce(listResponse(list, total));
}

type ListResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaCoreClient>['GET']>>;

function listResponse(list: InboundEventListItemDto[], total: number): ListResponse {
  return {
    data: { items: list, totalPages: Math.ceil(total / 10), totalCount: total },
    error: undefined,
    response: new Response(),
  };
}

function deferred() {
  let resolve: (value: ListResponse) => void = () => undefined;
  let reject: (reason: Error) => void = () => undefined;
  const promise = new Promise<ListResponse>((onResolve, onReject) => {
    resolve = onResolve;
    reject = onReject;
  });
  return { promise, resolve, reject };
}

describe('InboundEventStore', () => {
  it('that loading initially should result into the first page of ten entries', async () => {
    // given
    const { store, lunaCore } = createStore();
    respondWith(lunaCore, items(10), 25);

    // when
    await store.loadInitial();

    // then
    expect(lunaCore.GET).toHaveBeenCalledWith('/inbound-events', { params: { query: { page: 0, size: 10 } } });
    expect(store.items).toHaveLength(10);
    expect(store.totalCount).toBe(25);
    expect(store.hasMore).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that loading more should result into the next page without entries already shown', async () => {
    // given
    const { store, lunaCore } = createStore();
    respondWith(lunaCore, items(10), 21);
    await store.loadInitial();
    respondWith(lunaCore, [item('item-9'), ...items(10, 'older')], 21);

    // when
    await store.loadMore();

    // then
    expect(lunaCore.GET).toHaveBeenLastCalledWith('/inbound-events', {
      params: { query: { page: 1, size: 10 } },
    });
    expect(store.items).toHaveLength(20);
    expect(store.items.filter((entry) => entry.id === 'item-9')).toHaveLength(1);
  });

  it('that refreshing should result into reloading all visible entries', async () => {
    // given
    const { store, lunaCore } = createStore();
    respondWith(lunaCore, items(10), 30);
    await store.loadInitial();
    respondWith(lunaCore, items(10, 'older'), 30);
    await store.loadMore();

    // when
    await store.refresh();

    // then
    expect(lunaCore.GET).toHaveBeenLastCalledWith('/inbound-events', {
      params: { query: { page: 0, size: 20 } },
    });
  });

  it('that refreshing an empty list should result into the page size', async () => {
    // given
    const { store, lunaCore } = createStore();

    // when
    await store.refresh();

    // then
    expect(lunaCore.GET).toHaveBeenLastCalledWith('/inbound-events', {
      params: { query: { page: 0, size: 10 } },
    });
  });

  it('that refreshing more than one hundred entries should result into the largest page of luna-core', async () => {
    // given
    const { store, lunaCore } = createStore();
    respondWith(lunaCore, items(120), 130);
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(lunaCore.GET).toHaveBeenLastCalledWith('/inbound-events', {
      params: { query: { page: 0, size: 100 } },
    });
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.GET.mockResolvedValueOnce({
      data: undefined,
      error: undefined,
      response: new Response(null, { status: 500 }),
    });

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.GET.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
  });

  it('that loading after an error should result into a cleared load error', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.GET.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(store.loadError).toBe(false);
  });

  it('that submitting an input should result into a request and a reloaded list', async () => {
    // given
    const { store, lunaCore } = createStore();

    // when
    const accepted = await store.submit('Pay the invoice', 'TASK');

    // then
    expect(accepted).toBe(true);
    expect(lunaCore.POST).toHaveBeenCalledWith('/inbound-events/manual', {
      body: { text: 'Pay the invoice', area: 'PERSONAL', typeHint: 'TASK' },
    });
    expect(lunaCore.GET).toHaveBeenCalledTimes(1);
    expect(store.submitting).toBe(false);
    expect(store.submitError).toBeNull();
  });

  it('that submitting without type hint should result into a request without type hint', async () => {
    // given
    const { store, lunaCore } = createStore();

    // when
    await store.submit('Milk is empty', undefined);

    // then
    expect(lunaCore.POST).toHaveBeenCalledWith('/inbound-events/manual', {
      body: { text: 'Milk is empty', area: 'PERSONAL' },
    });
  });

  it('that a rejected input should result into an invalid input error', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.POST.mockResolvedValueOnce({
      data: { id: '' },
      error: undefined,
      response: new Response(null, { status: 400 }),
    });

    // when
    const accepted = await store.submit('x', undefined);

    // then
    expect(accepted).toBe(false);
    expect(store.submitError).toBe('invalid');
    expect(lunaCore.GET).not.toHaveBeenCalled();
  });

  it('that a server error should result into a failed submission', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.POST.mockResolvedValueOnce({
      data: { id: '' },
      error: undefined,
      response: new Response(null, { status: 503 }),
    });

    // when
    const accepted = await store.submit('Something', undefined);

    // then
    expect(accepted).toBe(false);
    expect(store.submitError).toBe('failed');
  });

  it('that an unreachable backend should result into a failed submission', async () => {
    // given
    const { store, lunaCore } = createStore();
    lunaCore.POST.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    // when
    const accepted = await store.submit('Something', undefined);

    // then
    expect(accepted).toBe(false);
    expect(store.submitError).toBe('failed');
    expect(store.submitting).toBe(false);
  });

  it('that a push signal should result into a refreshed list', () => {
    // given
    const { store, lunaCore, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'event-1' });

    // then
    expect(lunaCore.GET).toHaveBeenCalledTimes(1);
  });

  it('that connecting should result into one connection and a connected state without reload', () => {
    // given
    const { store, lunaCore, push } = createStore();

    // when
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('connected');
    expect(lunaCore.GET).not.toHaveBeenCalled();
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaCore, push } = createStore();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(stateWhileInterrupted).toBe('disconnected');
    expect(store.connection).toBe('connected');
    expect(lunaCore.GET).toHaveBeenCalledTimes(1);
  });

  it('that disconnecting should result into a closed connection and an idle state', async () => {
    // given
    const { store, push } = createStore();
    store.connect();

    // when
    await store.disconnect();

    // then
    expect(push.close).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('idle');
  });

  it('that disconnecting without connection should result into no error', async () => {
    // given
    const { store, push } = createStore();

    // when
    await store.disconnect();

    // then
    expect(push.close).not.toHaveBeenCalled();
  });

  it('that a store from a client without overrides should result into usable defaults', () => {
    // given
    const client = { GET: vi.fn(), POST: vi.fn() } as unknown as LunaCoreClient;

    // when
    const store = new InboundEventStore(client, fakePushConnector().connect, new LifeAreaStore());

    // then
    expect(store.items).toEqual([]);
    expect(store.connection).toBe('idle');
  });

  it('that a chosen life area should result into requests of this area only', async () => {
    // given
    const { store, lunaCore, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.loadInitial();

    // then
    expect(lunaCore.GET).toHaveBeenCalledWith('/inbound-events', {
      params: { query: { page: 0, size: 10, area: 'WORK' } },
    });
  });

  it('that another life area while the list is shown should result into reloading it for this area', async () => {
    // given
    const { store, lunaCore, lifeAreaStore } = createStore();
    lunaCore.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    store.connect();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    await vi.waitFor(() => {
      expect(lunaCore.GET).toHaveBeenLastCalledWith('/inbound-events', {
        params: { query: { page: 0, size: 10, area: 'WORK' } },
      });
    });
    expect(lunaCore.GET).toHaveBeenCalledTimes(2);
  });

  it('that another life area while the list is not shown should result into no request', () => {
    // given
    const { lunaCore, lifeAreaStore } = createStore();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    expect(lunaCore.GET).not.toHaveBeenCalled();
  });

  it('that submitting while one life area is shown should result into an input of this area', async () => {
    // given
    const { store, lunaCore, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.submit('Prepare the review', undefined);

    // then
    expect(lunaCore.POST).toHaveBeenCalledWith('/inbound-events/manual', {
      body: { text: 'Prepare the review', area: 'WORK' },
    });
  });

  it('that an outdated response should result into the list of the latest request', async () => {
    // given
    const { store, lunaCore } = createStore();
    const first = deferred();
    const second = deferred();
    lunaCore.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.loadInitial();
    const secondLoad = store.loadInitial();

    // when
    second.resolve(listResponse([item('newer')], 1));
    await secondLoad;
    first.resolve(listResponse([item('older')], 1));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['newer']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaCore } = createStore();
    const first = deferred();
    lunaCore.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(listResponse([item('newer')], 1));
    const firstLoad = store.loadInitial();
    await store.loadInitial();

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });
});
