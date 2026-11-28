import { describe, expect, it, vi } from 'vitest';

import type { AlertListDto, AlertListItemDto } from '../api';
import { fakeLunaAlertClient, fakePushConnector } from '../test';
import { LifeAreaStore } from './LifeAreaStore';
import { AlertListStore } from './AlertListStore';

type ListResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaAlertClient>['GET']>>;

function item(id: string, status: AlertListItemDto['status'] = 'OPEN'): AlertListItemDto {
  return {
    area: 'PERSONAL',
    id,
    title: `Alert ${id}`,
    severity: 'WARNING',
    status,
    createdDate: '2026-11-26T10:00:00Z',
  };
}

function items(count: number, prefix = 'alert'): AlertListItemDto[] {
  return Array.from({ length: count }, (_, index) => item(`${prefix}-${String(index)}`));
}

function listResponse(list: AlertListItemDto[], total: number): ListResponse {
  const data: AlertListDto = { items: list, totalPages: Math.ceil(total / 20), totalCount: total };
  return { data, error: undefined, response: new Response() };
}

function failedResponse(status: number): ListResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function createStore(
  lunaAlert = fakeLunaAlertClient(),
  push = fakePushConnector(),
  lifeAreaStore = new LifeAreaStore(),
) {
  return {
    store: new AlertListStore(lunaAlert.client, push.connect, lifeAreaStore),
    lunaAlert,
    push,
    lifeAreaStore,
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

describe('AlertListStore', () => {
  it('that loading initially should result into the first page of active alerts', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(listResponse(items(20), 45));

    // when
    await store.loadInitial();

    // then
    expect(lunaAlert.GET).toHaveBeenCalledWith('/alerts', {
      params: { query: { page: 0, size: 20, status: ['OPEN', 'ACKNOWLEDGED'] } },
    });
    expect(store.filter).toBe('ACTIVE');
    expect(store.items).toHaveLength(20);
    expect(store.totalCount).toBe(45);
    expect(store.hasMore).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that loading more should result into the next page without alerts already shown', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(listResponse(items(20), 41));
    await store.loadInitial();
    lunaAlert.GET.mockResolvedValueOnce(listResponse([item('alert-19'), ...items(20, 'later')], 41));

    // when
    await store.loadMore();

    // then
    expect(lunaAlert.GET).toHaveBeenLastCalledWith('/alerts', {
      params: { query: { page: 1, size: 20, status: ['OPEN', 'ACKNOWLEDGED'] } },
    });
    expect(store.items).toHaveLength(40);
  });

  it('that refreshing should result into reloading all visible alerts up to the largest page', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(listResponse(items(120), 120));
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(lunaAlert.GET).toHaveBeenLastCalledWith('/alerts', {
      params: { query: { page: 0, size: 100, status: ['OPEN', 'ACKNOWLEDGED'] } },
    });
  });

  it('that showing all alerts should result into a list without status filter', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    lunaAlert.GET.mockResolvedValueOnce(listResponse([item('resolved', 'RESOLVED')], 1));

    // when
    await store.changeFilter('ALL');

    // then
    expect(lunaAlert.GET).toHaveBeenLastCalledWith('/alerts', { params: { query: { page: 0, size: 20 } } });
    expect(store.filter).toBe('ALL');
    expect(store.items.map(({ id }) => id)).toEqual(['resolved']);
  });

  it('that choosing the current filter again should result into no request', async () => {
    // given
    const { store, lunaAlert } = createStore();

    // when
    await store.changeFilter('ACTIVE');

    // then
    expect(lunaAlert.GET).not.toHaveBeenCalled();
  });

  it('that an outdated response should result into the list of the latest request', async () => {
    // given
    const { store, lunaAlert } = createStore();
    const first = deferred();
    const second = deferred();
    lunaAlert.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.loadInitial();
    const secondLoad = store.changeFilter('RESOLVED');

    // when
    second.resolve(listResponse([item('resolved', 'RESOLVED')], 1));
    await secondLoad;
    first.resolve(listResponse([item('open')], 1));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['resolved']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaAlert } = createStore();
    const first = deferred();
    lunaAlert.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(
      listResponse([item('resolved', 'RESOLVED')], 1),
    );
    const firstLoad = store.loadInitial();
    await store.changeFilter('RESOLVED');

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error that a later load clears', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockRejectedValueOnce(new Error('network down'));
    await store.loadInitial();
    const errorBefore = store.loadError;

    // when
    await store.loadInitial();

    // then
    expect(errorBefore).toBe(true);
    expect(store.loadError).toBe(false);
  });

  it('that acknowledging an alert should result into a request and a reloaded list', async () => {
    // given
    const { store, lunaAlert } = createStore();
    const task = item('alert-1');

    // when
    const toggling = store.acknowledge(task);
    const pendingWhileRunning = store.isPending('alert-1');
    await toggling;

    // then
    expect(pendingWhileRunning).toBe(true);
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/acknowledge', { params: { path: { id: 'alert-1' } } });
    expect(lunaAlert.GET).toHaveBeenCalledTimes(1);
    expect(store.isPending('alert-1')).toBe(false);
    expect(store.actionError).toBe(false);
  });

  it('that a rejected status change should result into an action error without reload', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.POST.mockResolvedValueOnce({
      data: undefined,
      error: undefined,
      response: new Response(null, { status: 404 }),
    });

    // when
    await store.acknowledge(item('alert-1'));

    // then
    expect(store.actionError).toBe(true);
    expect(store.pendingIds).toEqual([]);
    expect(lunaAlert.GET).not.toHaveBeenCalled();
  });

  it('that an unreachable backend should result into an action error', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.acknowledge(item('alert-1'));

    // then
    expect(store.actionError).toBe(true);
    expect(store.isPending('alert-1')).toBe(false);
  });

  it('that a push signal should result into a refreshed list', () => {
    // given
    const { store, lunaAlert, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'alert-1' });

    // then
    expect(lunaAlert.GET).toHaveBeenCalledTimes(1);
  });

  it('that connecting twice should result into one connection without reload', () => {
    // given
    const { store, lunaAlert, push } = createStore();

    // when
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('connected');
    expect(lunaAlert.GET).not.toHaveBeenCalled();
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaAlert, push } = createStore();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(stateWhileInterrupted).toBe('disconnected');
    expect(store.connection).toBe('connected');
    expect(lunaAlert.GET).toHaveBeenCalledTimes(1);
  });

  it('that disconnecting should result into a closed connection and an idle state', async () => {
    // given
    const { store, push } = createStore();
    store.connect();

    // when
    await store.disconnect();
    await store.disconnect();

    // then
    expect(push.close).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('idle');
  });

  it('that a chosen life area should result into requests of this area only', async () => {
    // given
    const { store, lunaAlert, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.loadInitial();

    // then
    expect(lunaAlert.GET).toHaveBeenCalledWith('/alerts', {
      params: { query: { page: 0, size: 20, status: ['OPEN', 'ACKNOWLEDGED'], area: ['WORK'] } },
    });
  });

  it('that another life area while the list is shown should result into reloading it for this area', async () => {
    // given
    const { store, lunaAlert, lifeAreaStore } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    store.connect();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    await vi.waitFor(() => {
      expect(lunaAlert.GET).toHaveBeenLastCalledWith('/alerts', {
        params: { query: { page: 0, size: 20, status: ['OPEN', 'ACKNOWLEDGED'], area: ['WORK'] } },
      });
    });
    expect(lunaAlert.GET).toHaveBeenCalledTimes(2);
  });

  it('that another life area while the list is not shown should result into no request', () => {
    // given
    const { lunaAlert, lifeAreaStore } = createStore();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    expect(lunaAlert.GET).not.toHaveBeenCalled();
  });
});
