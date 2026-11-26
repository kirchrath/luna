import { describe, expect, it } from 'vitest';

import type { AppointmentListDto, AppointmentListItemDto } from '../api';
import { fakeLunaAppointmentClient, fakePushConnector } from '../test';
import { AppointmentListStore } from './AppointmentListStore';

type ListResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaAppointmentClient>['GET']>>;

function item(id: string, status: AppointmentListItemDto['status'] = 'SCHEDULED'): AppointmentListItemDto {
  return { id, title: `Appointment ${id}`, start: '2026-12-01T09:00:00Z', allDay: false, status, edited: false };
}

function items(count: number, prefix = 'appointment'): AppointmentListItemDto[] {
  return Array.from({ length: count }, (_, index) => item(`${prefix}-${String(index)}`));
}

function listResponse(list: AppointmentListItemDto[], total: number): ListResponse {
  const data: AppointmentListDto = { items: list, totalPages: Math.ceil(total / 20), totalCount: total };
  return { data, error: undefined, response: new Response() };
}

function failedResponse(status: number): ListResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function createStore(lunaAppointment = fakeLunaAppointmentClient(), push = fakePushConnector()) {
  return { store: new AppointmentListStore(lunaAppointment.client, push.connect), lunaAppointment, push };
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

describe('AppointmentListStore', () => {
  it('that loading initially should result into the first page of active appointments', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse(items(20), 45));

    // when
    await store.loadInitial();

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments', {
      params: { query: { page: 0, size: 20, period: 'UPCOMING' } },
    });
    expect(store.filter).toBe('UPCOMING');
    expect(store.items).toHaveLength(20);
    expect(store.totalCount).toBe(45);
    expect(store.hasMore).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that loading more should result into the next page without appointments already shown', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse(items(20), 41));
    await store.loadInitial();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse([item('appointment-19'), ...items(20, 'later')], 41));

    // when
    await store.loadMore();

    // then
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments', {
      params: { query: { page: 1, size: 20, period: 'UPCOMING' } },
    });
    expect(store.items).toHaveLength(40);
  });

  it('that refreshing should result into reloading all visible appointments up to the largest page', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse(items(120), 120));
    await store.loadInitial();

    // when
    await store.refresh();

    // then
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments', {
      params: { query: { page: 0, size: 100, period: 'UPCOMING' } },
    });
  });

  it('that showing past appointments should result into a list of the past period', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse(items(3), 3));
    await store.loadInitial();
    lunaAppointment.GET.mockResolvedValueOnce(listResponse([item('past')], 1));

    // when
    await store.changeFilter('PAST');

    // then
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments', {
      params: { query: { page: 0, size: 20, period: 'PAST' } },
    });
    expect(store.filter).toBe('PAST');
    expect(store.items.map(({ id }) => id)).toEqual(['past']);
  });

  it('that choosing the current filter again should result into no request', async () => {
    // given
    const { store, lunaAppointment } = createStore();

    // when
    await store.changeFilter('UPCOMING');

    // then
    expect(lunaAppointment.GET).not.toHaveBeenCalled();
  });

  it('that an outdated response should result into the list of the latest request', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    const first = deferred();
    const second = deferred();
    lunaAppointment.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.loadInitial();
    const secondLoad = store.changeFilter('PAST');

    // when
    second.resolve(listResponse([item('past')], 1));
    await secondLoad;
    first.resolve(listResponse([item('upcoming')], 1));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['past']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    const first = deferred();
    lunaAppointment.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(listResponse([item('past')], 1));
    const firstLoad = store.loadInitial();
    await store.changeFilter('PAST');

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.loadInitial();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error that a later load clears', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));
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
    const { store, lunaAppointment, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'appointment-1' });

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(1);
  });

  it('that connecting twice should result into one connection without reload', () => {
    // given
    const { store, lunaAppointment, push } = createStore();

    // when
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
    expect(store.connection).toBe('connected');
    expect(lunaAppointment.GET).not.toHaveBeenCalled();
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaAppointment, push } = createStore();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(stateWhileInterrupted).toBe('disconnected');
    expect(store.connection).toBe('connected');
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(1);
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
});
