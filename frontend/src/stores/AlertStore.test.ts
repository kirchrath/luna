import { describe, expect, it } from 'vitest';

import type { AlertDto } from '../api';
import { fakeLunaAlertClient, ALERT } from '../test';
import { AlertStore } from './AlertStore';

type TaskResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaAlertClient>['PUT']>>;

function taskResponse(task: AlertDto): TaskResponse {
  return { data: task, error: undefined, response: new Response() };
}

function failedResponse(status: number): TaskResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function withVersion(version: number): AlertDto {
  return { ...ALERT, audit: { ...ALERT.audit, version } };
}

function createStore(lunaAlert = fakeLunaAlertClient()) {
  return { store: new AlertStore(lunaAlert.client), lunaAlert };
}

async function loadedStore(lunaAlert = fakeLunaAlertClient()) {
  const created = createStore(lunaAlert);
  await created.store.load('alert-1');
  return created;
}

describe('AlertStore', () => {
  it('that loading an alert should result into the loaded alert', async () => {
    // given
    const { store, lunaAlert } = createStore();

    // when
    const loading = store.load('alert-1');
    const stateWhileLoading = store.loadState;
    await loading;

    // then
    expect(stateWhileLoading).toBe('loading');
    expect(lunaAlert.GET).toHaveBeenCalledWith('/alerts/{id}', { params: { path: { id: 'alert-1' } } });
    expect(store.id).toBe('alert-1');
    expect(store.alert).toEqual(ALERT);
    expect(store.loadState).toBe('loaded');
  });

  it('that loading an unknown alert should result into not found', async () => {
    // given
    const { store } = createStore(fakeLunaAlertClient(undefined, null));

    // when
    await store.load('unknown');

    // then
    expect(store.alert).toBeNull();
    expect(store.loadState).toBe('notFound');
  });

  it('that a server error while loading should result into a failed load', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.load('alert-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that an unreachable backend while loading should result into a failed load', async () => {
    // given
    const { store, lunaAlert } = createStore();
    lunaAlert.GET.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.load('alert-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that loading another alert should result into no task of the previous one meanwhile', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();
    let resolveSecond: (response: TaskResponse) => void = () => undefined;
    lunaAlert.GET.mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    // when
    const loading = store.load('alert-2');
    const taskWhileLoading = store.alert;
    resolveSecond(taskResponse(withVersion(7)));
    await loading;

    // then
    expect(taskWhileLoading).toBeNull();
    expect(store.alert?.audit.version).toBe(7);
  });

  it('that a late answer for a previous alert should result into the latest alert only', async () => {
    // given
    const { store, lunaAlert } = createStore();
    let resolveFirst: (response: TaskResponse) => void = () => undefined;
    let rejectOther: (error: Error) => void = () => undefined;
    lunaAlert.GET.mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((_, reject) => (rejectOther = reject)))
      .mockResolvedValueOnce(taskResponse(withVersion(9)));
    const first = store.load('alert-1');
    const other = store.load('alert-2');
    await store.load('alert-3');

    // when
    resolveFirst(taskResponse(withVersion(1)));
    rejectOther(new Error('network down'));
    await Promise.all([first, other]);

    // then
    expect(store.id).toBe('alert-3');
    expect(store.alert?.audit.version).toBe(9);
    expect(store.loadState).toBe('loaded');
  });

  it('that reloading should result into loading the current alert again', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();

    // when
    await store.reload();

    // then
    expect(lunaAlert.GET).toHaveBeenCalledTimes(2);
  });

  it('that reloading without alert should result into no request', async () => {
    // given
    const { store, lunaAlert } = createStore();

    // when
    await store.reload();

    // then
    expect(lunaAlert.GET).not.toHaveBeenCalled();
  });

  it('that acknowledging should result into the acknowledged alert', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();
    const completed: AlertDto = { ...withVersion(4), data: { ...ALERT.data, status: 'ACKNOWLEDGED' } };
    lunaAlert.POST.mockResolvedValueOnce(taskResponse(completed));

    // when
    const completing = store.acknowledge();
    const changingWhileRunning = store.statusChanging;
    await completing;

    // then
    expect(changingWhileRunning).toBe(true);
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/acknowledge', { params: { path: { id: 'alert-1' } } });
    expect(store.alert?.data.status).toBe('ACKNOWLEDGED');
    expect(store.statusChanging).toBe(false);
  });

  it('that resolving should result into a request to resolve', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();

    // when
    await store.resolve();

    // then
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/resolve', { params: { path: { id: 'alert-1' } } });
  });

  it('that reopening should result into a request to reopen', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();

    // when
    await store.reopen();

    // then
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/reopen', { params: { path: { id: 'alert-1' } } });
    expect(store.statusError).toBe(false);
  });

  it('that a rejected status change should result into a status error', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();
    lunaAlert.POST.mockResolvedValueOnce(failedResponse(404));

    // when
    await store.acknowledge();

    // then
    expect(store.statusError).toBe(true);
    expect(store.alert).toEqual(ALERT);
  });

  it('that an unreachable backend while changing the status should result into a status error', async () => {
    // given
    const { store, lunaAlert } = await loadedStore();
    lunaAlert.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.reopen();

    // then
    expect(store.statusError).toBe(true);
    expect(store.statusChanging).toBe(false);
  });

  it('that changing the status without alert should result into no request', async () => {
    // given
    const { store, lunaAlert } = createStore();

    // when
    await store.acknowledge();

    // then
    expect(lunaAlert.POST).not.toHaveBeenCalled();
  });
});
