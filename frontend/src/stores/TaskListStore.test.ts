import { describe, expect, it, vi } from 'vitest';

import type { TaskListDto, TaskListItemDto } from '../api';
import { fakeLunaTaskClient, fakePushConnector } from '../test';
import { LifeAreaStore } from './LifeAreaStore';
import { TaskListStore } from './TaskListStore';

type ListResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaTaskClient>['GET']>>;

function item(id: string, status: TaskListItemDto['status'] = 'OPEN'): TaskListItemDto {
  return { area: 'PERSONAL', id, title: `Task ${id}`, priority: 'NORMAL', status, edited: false };
}

function items(count: number, prefix = 'task'): TaskListItemDto[] {
  return Array.from({ length: count }, (_, index) => item(`${prefix}-${String(index)}`));
}

function listResponse(list: TaskListItemDto[], total: number): ListResponse {
  const data: TaskListDto = { items: list, totalPages: Math.ceil(total / 20), totalCount: total };
  return { data, error: undefined, response: new Response() };
}

function failedResponse(status: number): ListResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function createStore(lunaTask = fakeLunaTaskClient(), push = fakePushConnector(), lifeAreaStore = new LifeAreaStore()) {
  return {
    store: new TaskListStore(lunaTask.client, push.connect, lifeAreaStore),
    lunaTask,
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

describe('TaskListStore', () => {
  it('that loading initially should result into the first page of open tasks', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(listResponse(items(20), 45));

    // when
    await store.loadInitial();

    // then
    expect(lunaTask.GET).toHaveBeenCalledWith('/tasks', { params: { query: { page: 0, size: 20, status: 'OPEN' } } });
    expect(store.filter).toBe('OPEN');
    expect(store.items).toHaveLength(20);
    expect(store.totalCount).toBe(45);
    expect(store.hasMore).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that loading more should result into the next page without tasks already shown', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(listResponse(items(20), 41));
    await store.loadInitial();
    lunaTask.GET.mockResolvedValueOnce(listResponse([item('task-19'), ...items(20, 'later')], 41));

    // when
    await store.loadMore();

    // then
    expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', {
      params: { query: { page: 1, size: 20, status: 'OPEN' } },
    });
    expect(store.items).toHaveLength(40);
  });

  it('that refreshing should result into reloading all visible tasks up to the largest page', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(listResponse(items(120), 120));
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', {
      params: { query: { page: 0, size: 100, status: 'OPEN' } },
    });
  });

  it('that showing all tasks should result into a list without status filter', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    lunaTask.GET.mockResolvedValueOnce(listResponse([item('done', 'DONE')], 1));

    // when
    await store.changeFilter('ALL');

    // then
    expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', { params: { query: { page: 0, size: 20 } } });
    expect(store.filter).toBe('ALL');
    expect(store.items.map(({ id }) => id)).toEqual(['done']);
  });

  it('that choosing the current filter again should result into no request', async () => {
    // given
    const { store, lunaTask } = createStore();

    // when
    await store.changeFilter('OPEN');

    // then
    expect(lunaTask.GET).not.toHaveBeenCalled();
  });

  it('that an outdated response should result into the list of the latest request', async () => {
    // given
    const { store, lunaTask } = createStore();
    const first = deferred();
    const second = deferred();
    lunaTask.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.loadInitial();
    const secondLoad = store.changeFilter('DONE');

    // when
    second.resolve(listResponse([item('done', 'DONE')], 1));
    await secondLoad;
    first.resolve(listResponse([item('open')], 1));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['done']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaTask } = createStore();
    const first = deferred();
    lunaTask.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(listResponse([item('done', 'DONE')], 1));
    const firstLoad = store.loadInitial();
    await store.changeFilter('DONE');

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error that a later load clears', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockRejectedValueOnce(new Error('network down'));
    await store.loadInitial();
    const errorBefore = store.loadError;

    // when
    await store.loadInitial();

    // then
    expect(errorBefore).toBe(true);
    expect(store.loadError).toBe(false);
  });

  it('that ticking an open task should result into completing it and a reloaded list', async () => {
    // given
    const { store, lunaTask } = createStore();
    const task = item('task-1');

    // when
    const toggling = store.toggle(task);
    const pendingWhileRunning = store.isPending('task-1');
    await toggling;

    // then
    expect(pendingWhileRunning).toBe(true);
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/complete', { params: { path: { id: 'task-1' } } });
    expect(lunaTask.GET).toHaveBeenCalledTimes(1);
    expect(store.isPending('task-1')).toBe(false);
    expect(store.actionError).toBe(false);
  });

  it('that unticking a done task should result into reopening it', async () => {
    // given
    const { store, lunaTask } = createStore();

    // when
    await store.toggle(item('task-1', 'DONE'));

    // then
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/reopen', { params: { path: { id: 'task-1' } } });
  });

  it('that a rejected status change should result into an action error without reload', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.POST.mockResolvedValueOnce({
      data: undefined,
      error: undefined,
      response: new Response(null, { status: 404 }),
    });

    // when
    await store.toggle(item('task-1'));

    // then
    expect(store.actionError).toBe(true);
    expect(store.pendingIds).toEqual([]);
    expect(lunaTask.GET).not.toHaveBeenCalled();
  });

  it('that an unreachable backend should result into an action error', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.toggle(item('task-1'));

    // then
    expect(store.actionError).toBe(true);
    expect(store.isPending('task-1')).toBe(false);
  });

  it('that a push signal should result into a refreshed list', () => {
    // given
    const { store, lunaTask, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'task-1' });

    // then
    expect(lunaTask.GET).toHaveBeenCalledTimes(1);
  });

  it('that connecting twice should result into one connection without reload', () => {
    // given
    const { store, lunaTask, push } = createStore();

    // when
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('connected');
    expect(lunaTask.GET).not.toHaveBeenCalled();
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaTask, push } = createStore();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(stateWhileInterrupted).toBe('disconnected');
    expect(store.connection).toBe('connected');
    expect(lunaTask.GET).toHaveBeenCalledTimes(1);
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
    const { store, lunaTask, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.loadInitial();

    // then
    expect(lunaTask.GET).toHaveBeenCalledWith('/tasks', {
      params: { query: { page: 0, size: 20, status: 'OPEN', area: 'WORK' } },
    });
  });

  it('that another life area while the list is shown should result into reloading it for this area', async () => {
    // given
    const { store, lunaTask, lifeAreaStore } = createStore();
    lunaTask.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    store.connect();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    await vi.waitFor(() => {
      expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', {
        params: { query: { page: 0, size: 20, status: 'OPEN', area: 'WORK' } },
      });
    });
    expect(lunaTask.GET).toHaveBeenCalledTimes(2);
  });

  it('that another life area while the list is not shown should result into no request', () => {
    // given
    const { lunaTask, lifeAreaStore } = createStore();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    expect(lunaTask.GET).not.toHaveBeenCalled();
  });
});
