import { describe, expect, it, vi } from 'vitest';

import type { NoteListDto, NoteListItemDto } from '../api';
import { fakeLunaNoteClient, fakePushConnector } from '../test';
import { LifeAreaStore } from './LifeAreaStore';
import { NoteListStore } from './NoteListStore';

type ListResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaNoteClient>['GET']>>;

function item(id: string, status: NoteListItemDto['status'] = 'ACTIVE'): NoteListItemDto {
  return {
    area: 'PERSONAL',
    id,
    title: `Note ${id}`,
    summary: 'Summary',
    status,
    edited: false,
    createdDate: '2026-11-26T10:00:00Z',
  };
}

function items(count: number, prefix = 'note'): NoteListItemDto[] {
  return Array.from({ length: count }, (_, index) => item(`${prefix}-${String(index)}`));
}

function listResponse(list: NoteListItemDto[], total: number): ListResponse {
  const data: NoteListDto = { items: list, totalPages: Math.ceil(total / 20), totalCount: total };
  return { data, error: undefined, response: new Response() };
}

function failedResponse(status: number): ListResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function createStore(lunaNote = fakeLunaNoteClient(), push = fakePushConnector(), lifeAreaStore = new LifeAreaStore()) {
  return {
    store: new NoteListStore(lunaNote.client, push.connect, lifeAreaStore),
    lunaNote,
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

describe('NoteListStore', () => {
  it('that loading initially should result into the first page of active notes', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(listResponse(items(20), 45));

    // when
    await store.loadInitial();

    // then
    expect(lunaNote.GET).toHaveBeenCalledWith('/notes', { params: { query: { page: 0, size: 20, status: 'ACTIVE' } } });
    expect(store.filter).toBe('ACTIVE');
    expect(store.items).toHaveLength(20);
    expect(store.totalCount).toBe(45);
    expect(store.hasMore).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that loading more should result into the next page without notes already shown', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(listResponse(items(20), 41));
    await store.loadInitial();
    lunaNote.GET.mockResolvedValueOnce(listResponse([item('note-19'), ...items(20, 'later')], 41));

    // when
    await store.loadMore();

    // then
    expect(lunaNote.GET).toHaveBeenLastCalledWith('/notes', {
      params: { query: { page: 1, size: 20, status: 'ACTIVE' } },
    });
    expect(store.items).toHaveLength(40);
  });

  it('that refreshing should result into reloading all visible notes up to the largest page', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(listResponse(items(120), 120));
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(lunaNote.GET).toHaveBeenLastCalledWith('/notes', {
      params: { query: { page: 0, size: 100, status: 'ACTIVE' } },
    });
  });

  it('that showing all notes should result into a list without status filter', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    lunaNote.GET.mockResolvedValueOnce(listResponse([item('archived', 'ARCHIVED')], 1));

    // when
    await store.changeFilter('ALL');

    // then
    expect(lunaNote.GET).toHaveBeenLastCalledWith('/notes', { params: { query: { page: 0, size: 20 } } });
    expect(store.filter).toBe('ALL');
    expect(store.items.map(({ id }) => id)).toEqual(['archived']);
  });

  it('that choosing the current filter again should result into no request', async () => {
    // given
    const { store, lunaNote } = createStore();

    // when
    await store.changeFilter('ACTIVE');

    // then
    expect(lunaNote.GET).not.toHaveBeenCalled();
  });

  it('that an outdated response should result into the list of the latest request', async () => {
    // given
    const { store, lunaNote } = createStore();
    const first = deferred();
    const second = deferred();
    lunaNote.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.loadInitial();
    const secondLoad = store.changeFilter('ARCHIVED');

    // when
    second.resolve(listResponse([item('archived', 'ARCHIVED')], 1));
    await secondLoad;
    first.resolve(listResponse([item('active')], 1));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['archived']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaNote } = createStore();
    const first = deferred();
    lunaNote.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(
      listResponse([item('archived', 'ARCHIVED')], 1),
    );
    const firstLoad = store.loadInitial();
    await store.changeFilter('ARCHIVED');

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error that a later load clears', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockRejectedValueOnce(new Error('network down'));
    await store.loadInitial();
    const errorBefore = store.loadError;

    // when
    await store.loadInitial();

    // then
    expect(errorBefore).toBe(true);
    expect(store.loadError).toBe(false);
  });

  it('that a push signal should result into a refreshed list', () => {
    // given
    const { store, lunaNote, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'note-1' });

    // then
    expect(lunaNote.GET).toHaveBeenCalledTimes(1);
  });

  it('that connecting twice should result into one connection without reload', () => {
    // given
    const { store, lunaNote, push } = createStore();

    // when
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('connected');
    expect(lunaNote.GET).not.toHaveBeenCalled();
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaNote, push } = createStore();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(stateWhileInterrupted).toBe('disconnected');
    expect(store.connection).toBe('connected');
    expect(lunaNote.GET).toHaveBeenCalledTimes(1);
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
    const { store, lunaNote, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.loadInitial();

    // then
    expect(lunaNote.GET).toHaveBeenCalledWith('/notes', {
      params: { query: { page: 0, size: 20, status: 'ACTIVE', area: ['WORK'] } },
    });
  });

  it('that another life area while the list is shown should result into reloading it for this area', async () => {
    // given
    const { store, lunaNote, lifeAreaStore } = createStore();
    lunaNote.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    store.connect();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    await vi.waitFor(() => {
      expect(lunaNote.GET).toHaveBeenLastCalledWith('/notes', {
        params: { query: { page: 0, size: 20, status: 'ACTIVE', area: ['WORK'] } },
      });
    });
    expect(lunaNote.GET).toHaveBeenCalledTimes(2);
  });

  it('that another life area while the list is not shown should result into no request', () => {
    // given
    const { lunaNote, lifeAreaStore } = createStore();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    expect(lunaNote.GET).not.toHaveBeenCalled();
  });
});
