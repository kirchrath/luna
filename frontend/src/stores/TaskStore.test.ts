import { describe, expect, it } from 'vitest';

import type { TaskDto } from '../api';
import { fakeLunaTaskClient, TASK } from '../test';
import { TaskStore } from './TaskStore';

type TaskResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaTaskClient>['PUT']>>;

const CHANGES = { title: 'Pay the gas bill', priority: 'LOW' } as const;

function taskResponse(task: TaskDto): TaskResponse {
  return { data: task, error: undefined, response: new Response() };
}

function failedResponse(status: number): TaskResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function withVersion(version: number): TaskDto {
  return { ...TASK, audit: { ...TASK.audit, version } };
}

function createStore(lunaTask = fakeLunaTaskClient()) {
  return { store: new TaskStore(lunaTask.client), lunaTask };
}

async function loadedStore(lunaTask = fakeLunaTaskClient()) {
  const created = createStore(lunaTask);
  await created.store.load('task-1');
  return created;
}

describe('TaskStore', () => {
  it('that loading a task should result into the loaded task', async () => {
    // given
    const { store, lunaTask } = createStore();

    // when
    const loading = store.load('task-1');
    const stateWhileLoading = store.loadState;
    await loading;

    // then
    expect(stateWhileLoading).toBe('loading');
    expect(lunaTask.GET).toHaveBeenCalledWith('/tasks/{id}', { params: { path: { id: 'task-1' } } });
    expect(store.id).toBe('task-1');
    expect(store.task).toEqual(TASK);
    expect(store.loadState).toBe('loaded');
  });

  it('that loading an unknown task should result into not found', async () => {
    // given
    const { store } = createStore(fakeLunaTaskClient(undefined, null));

    // when
    await store.load('unknown');

    // then
    expect(store.task).toBeNull();
    expect(store.loadState).toBe('notFound');
  });

  it('that a server error while loading should result into a failed load', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.load('task-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that an unreachable backend while loading should result into a failed load', async () => {
    // given
    const { store, lunaTask } = createStore();
    lunaTask.GET.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.load('task-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that loading another task should result into no task of the previous one meanwhile', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    let resolveSecond: (response: TaskResponse) => void = () => undefined;
    lunaTask.GET.mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    // when
    const loading = store.load('task-2');
    const taskWhileLoading = store.task;
    resolveSecond(taskResponse(withVersion(7)));
    await loading;

    // then
    expect(taskWhileLoading).toBeNull();
    expect(store.task?.audit.version).toBe(7);
  });

  it('that a late answer for a previous task should result into the latest task only', async () => {
    // given
    const { store, lunaTask } = createStore();
    let resolveFirst: (response: TaskResponse) => void = () => undefined;
    let rejectOther: (error: Error) => void = () => undefined;
    lunaTask.GET.mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((_, reject) => (rejectOther = reject)))
      .mockResolvedValueOnce(taskResponse(withVersion(9)));
    const first = store.load('task-1');
    const other = store.load('task-2');
    await store.load('task-3');

    // when
    resolveFirst(taskResponse(withVersion(1)));
    rejectOther(new Error('network down'));
    await Promise.all([first, other]);

    // then
    expect(store.id).toBe('task-3');
    expect(store.task?.audit.version).toBe(9);
    expect(store.loadState).toBe('loaded');
  });

  it('that reloading should result into loading the current task again', async () => {
    // given
    const { store, lunaTask } = await loadedStore();

    // when
    await store.reload();

    // then
    expect(lunaTask.GET).toHaveBeenCalledTimes(2);
  });

  it('that reloading without task should result into no request', async () => {
    // given
    const { store, lunaTask } = createStore();

    // when
    await store.reload();

    // then
    expect(lunaTask.GET).not.toHaveBeenCalled();
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    lunaTask.PUT.mockResolvedValueOnce(taskResponse(withVersion(4)));

    // when
    const saving = store.save(CHANGES);
    const savingWhileRunning = store.saving;
    const saved = await saving;

    // then
    expect(savingWhileRunning).toBe(true);
    expect(saved).toBe(true);
    expect(lunaTask.PUT).toHaveBeenCalledWith('/tasks/{id}', {
      params: { path: { id: 'task-1' } },
      body: { ...CHANGES, version: 3 },
    });
    expect(store.task?.audit.version).toBe(4);
    expect(store.saving).toBe(false);
    expect(store.saveError).toBeNull();
  });

  it.each([
    [409, 'conflict'],
    [400, 'invalid'],
    [500, 'failed'],
  ] as const)('that a status %i while saving should result into the save error %s', async (status, error) => {
    // given
    const { store, lunaTask } = await loadedStore();
    lunaTask.PUT.mockResolvedValueOnce(failedResponse(status));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe(error);
    expect(store.task).toEqual(TASK);
  });

  it('that an unreachable backend while saving should result into a failed save', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    lunaTask.PUT.mockRejectedValueOnce(new Error('network down'));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe('failed');
  });

  it('that saving without loaded task should result into no request', async () => {
    // given
    const { store, lunaTask } = createStore(fakeLunaTaskClient(undefined, null));
    await store.load('unknown');

    // when
    const saved = await store.save(CHANGES);
    const savedWithoutId = await createStore(lunaTask).store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(savedWithoutId).toBe(false);
    expect(lunaTask.PUT).not.toHaveBeenCalled();
  });

  it('that completing should result into the completed task', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    const completed: TaskDto = { ...withVersion(4), data: { ...TASK.data, status: 'DONE' } };
    lunaTask.POST.mockResolvedValueOnce(taskResponse(completed));

    // when
    const completing = store.complete();
    const changingWhileRunning = store.statusChanging;
    await completing;

    // then
    expect(changingWhileRunning).toBe(true);
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/complete', { params: { path: { id: 'task-1' } } });
    expect(store.task?.data.status).toBe('DONE');
    expect(store.statusChanging).toBe(false);
  });

  it('that reopening should result into a request to reopen', async () => {
    // given
    const { store, lunaTask } = await loadedStore();

    // when
    await store.reopen();

    // then
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/reopen', { params: { path: { id: 'task-1' } } });
    expect(store.statusError).toBe(false);
  });

  it('that a rejected status change should result into a status error', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    lunaTask.POST.mockResolvedValueOnce(failedResponse(404));

    // when
    await store.complete();

    // then
    expect(store.statusError).toBe(true);
    expect(store.task).toEqual(TASK);
  });

  it('that an unreachable backend while changing the status should result into a status error', async () => {
    // given
    const { store, lunaTask } = await loadedStore();
    lunaTask.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.reopen();

    // then
    expect(store.statusError).toBe(true);
    expect(store.statusChanging).toBe(false);
  });

  it('that changing the status without task should result into no request', async () => {
    // given
    const { store, lunaTask } = createStore();

    // when
    await store.complete();

    // then
    expect(lunaTask.POST).not.toHaveBeenCalled();
  });
});
