import { describe, expect, it, vi } from 'vitest';

import type { AppointmentListDto, AppointmentListItemDto } from '../api';
import { fakeLunaAppointmentClient, fakePushConnector } from '../test';
import { LifeAreaStore } from './LifeAreaStore';
import { AppointmentMonthStore } from './AppointmentMonthStore';

type RangeResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaAppointmentClient>['GET']>>;

const TODAY = new Date(2026, 10, 26, 15, 30);

function item(id: string): AppointmentListItemDto {
  return {
    area: 'PERSONAL',
    id,
    title: `Appointment ${id}`,
    start: '2026-11-27T09:00:00Z',
    allDay: false,
    status: 'SCHEDULED',
    edited: false,
  };
}

function rangeResponse(items: AppointmentListItemDto[]): RangeResponse {
  const data: AppointmentListDto = { items, totalPages: 1, totalCount: items.length };
  return { data, error: undefined, response: new Response() };
}

function failedResponse(status: number): RangeResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function createStore(
  lunaAppointment = fakeLunaAppointmentClient(),
  push = fakePushConnector(),
  lifeAreaStore = new LifeAreaStore(),
) {
  return {
    store: new AppointmentMonthStore(lunaAppointment.client, push.connect, lifeAreaStore, TODAY),
    lunaAppointment,
    push,
    lifeAreaStore,
  };
}

function deferred() {
  let resolve: (value: RangeResponse) => void = () => undefined;
  let reject: (reason: Error) => void = () => undefined;
  const promise = new Promise<RangeResponse>((onResolve, onReject) => {
    resolve = onResolve;
    reject = onReject;
  });
  return { promise, resolve, reject };
}

describe('AppointmentMonthStore', () => {
  it('that loading should result into the appointments of the current month', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(rangeResponse([item('a')]));

    // when
    const loading = store.load();
    const loadingWhileRunning = store.loading;
    await loading;

    // then
    expect(loadingWhileRunning).toBe(true);
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments/range', {
      params: {
        query: { from: new Date(2026, 10, 1).toISOString(), to: new Date(2026, 11, 1).toISOString() },
      },
    });
    expect(store.month).toEqual(new Date(2026, 10, 1));
    expect(store.items.map(({ id }) => id)).toEqual(['a']);
    expect(store.loading).toBe(false);
    expect(store.loadError).toBe(false);
  });

  it('that creating the store without a day should result into the month of today', () => {
    // given
    const lunaAppointment = fakeLunaAppointmentClient();
    const now = new Date();

    // when
    const store = new AppointmentMonthStore(lunaAppointment.client, fakePushConnector().connect, new LifeAreaStore());

    // then
    expect(store.month).toEqual(new Date(now.getFullYear(), now.getMonth(), 1));
    expect(store.selectedDay).toBeNull();
  });

  it('that going to the next and previous month should result into loading those months without selection', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    store.selectDay(new Date(2026, 10, 3));

    // when
    await store.nextMonth();
    const afterNext = store.month;
    const selectionAfterNext = store.selectedDay;
    await store.previousMonth();
    await store.previousMonth();

    // then
    expect(afterNext).toEqual(new Date(2026, 11, 1));
    expect(selectionAfterNext).toBeNull();
    expect(store.month).toEqual(new Date(2026, 9, 1));
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(3);
  });

  it('that showing today should result into the month of today with today selected', async () => {
    // given
    const { store } = createStore();
    await store.nextMonth();

    // when
    await store.showToday(new Date(2026, 10, 26, 18, 0));

    // then
    expect(store.month).toEqual(new Date(2026, 10, 1));
    expect(store.selectedDay).toEqual(new Date(2026, 10, 26));
  });

  it('that showing today without a day should result into the month of the current day', async () => {
    // given
    const { store } = createStore();
    const now = new Date();

    // when
    await store.showToday();

    // then
    expect(store.selectedDay).toEqual(new Date(now.getFullYear(), now.getMonth(), now.getDate()));
  });

  it('that a failed response should result into a load error', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.load();

    // then
    expect(store.loadError).toBe(true);
    expect(store.items).toEqual([]);
    expect(store.loading).toBe(false);
  });

  it('that an unreachable backend should result into a load error', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.load();

    // then
    expect(store.loadError).toBe(true);
    expect(store.loading).toBe(false);
  });

  it('that an outdated response should result into the appointments of the latest month', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    const first = deferred();
    const second = deferred();
    lunaAppointment.GET.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const firstLoad = store.load();
    const secondLoad = store.nextMonth();

    // when
    second.resolve(rangeResponse([item('december')]));
    await secondLoad;
    first.resolve(rangeResponse([item('november')]));
    await firstLoad;

    // then
    expect(store.items.map(({ id }) => id)).toEqual(['december']);
    expect(store.loading).toBe(false);
  });

  it('that an outdated failure should result into no load error', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    const first = deferred();
    lunaAppointment.GET.mockReturnValueOnce(first.promise).mockResolvedValueOnce(rangeResponse([item('december')]));
    const firstLoad = store.load();
    await store.nextMonth();

    // when
    first.reject(new Error('network down'));
    await firstLoad;

    // then
    expect(store.loadError).toBe(false);
    expect(store.items).toHaveLength(1);
  });

  it('that a push signal should result into a reloaded month', () => {
    // given
    const { store, lunaAppointment, push } = createStore();
    store.connect();

    // when
    push.handlers[0]?.onSignal({ id: 'appointment-1' });

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(1);
  });

  it('that an interrupted connection should result into a disconnected state and a reload after reconnecting', () => {
    // given
    const { store, lunaAppointment, push } = createStore();
    store.connect();
    store.connect();
    push.handlers[0]?.onConnected();

    // when
    push.handlers[0]?.onDisconnected();
    const stateWhileInterrupted = store.connection;
    push.handlers[0]?.onConnected();

    // then
    expect(push.connect).toHaveBeenCalledTimes(1);
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

  it('that a chosen life area should result into requests of this area only', async () => {
    // given
    const { store, lunaAppointment, lifeAreaStore } = createStore();
    lifeAreaStore.changeFilter('WORK');

    // when
    await store.load();

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments/range', {
      params: {
        query: { from: new Date(2026, 10, 1).toISOString(), to: new Date(2026, 11, 1).toISOString(), area: 'WORK' },
      },
    });
  });

  it('that another life area while the list is shown should result into reloading it for this area', async () => {
    // given
    const { store, lunaAppointment, lifeAreaStore } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(rangeResponse([item('a')]));
    await store.load();
    store.connect();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    await vi.waitFor(() => {
      expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments/range', {
        params: {
          query: { from: new Date(2026, 10, 1).toISOString(), to: new Date(2026, 11, 1).toISOString(), area: 'WORK' },
        },
      });
    });
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(2);
  });

  it('that another life area while the list is not shown should result into no request', () => {
    // given
    const { lunaAppointment, lifeAreaStore } = createStore();

    // when
    lifeAreaStore.changeFilter('WORK');

    // then
    expect(lunaAppointment.GET).not.toHaveBeenCalled();
  });
});
