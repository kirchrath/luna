import { describe, expect, it } from 'vitest';

import type { NoteDto } from '../api';
import { fakeLunaNoteClient, NOTE } from '../test';
import { NoteStore } from './NoteStore';

type TaskResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaNoteClient>['PUT']>>;

const CHANGES = { title: 'Owners meeting 2026', summary: 'Next meeting in October' } as const;

function taskResponse(task: NoteDto): TaskResponse {
  return { data: task, error: undefined, response: new Response() };
}

function failedResponse(status: number): TaskResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function withVersion(version: number): NoteDto {
  return { ...NOTE, audit: { ...NOTE.audit, version } };
}

function createStore(lunaNote = fakeLunaNoteClient()) {
  return { store: new NoteStore(lunaNote.client), lunaNote };
}

async function loadedStore(lunaNote = fakeLunaNoteClient()) {
  const created = createStore(lunaNote);
  await created.store.load('note-1');
  return created;
}

describe('NoteStore', () => {
  it('that loading a note should result into the loaded note', async () => {
    // given
    const { store, lunaNote } = createStore();

    // when
    const loading = store.load('note-1');
    const stateWhileLoading = store.loadState;
    await loading;

    // then
    expect(stateWhileLoading).toBe('loading');
    expect(lunaNote.GET).toHaveBeenCalledWith('/notes/{id}', { params: { path: { id: 'note-1' } } });
    expect(store.id).toBe('note-1');
    expect(store.note).toEqual(NOTE);
    expect(store.loadState).toBe('loaded');
  });

  it('that loading an unknown note should result into not found', async () => {
    // given
    const { store } = createStore(fakeLunaNoteClient(undefined, null));

    // when
    await store.load('unknown');

    // then
    expect(store.note).toBeNull();
    expect(store.loadState).toBe('notFound');
  });

  it('that a server error while loading should result into a failed load', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.load('note-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that an unreachable backend while loading should result into a failed load', async () => {
    // given
    const { store, lunaNote } = createStore();
    lunaNote.GET.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.load('note-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that loading another note should result into no task of the previous one meanwhile', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    let resolveSecond: (response: TaskResponse) => void = () => undefined;
    lunaNote.GET.mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    // when
    const loading = store.load('note-2');
    const taskWhileLoading = store.note;
    resolveSecond(taskResponse(withVersion(7)));
    await loading;

    // then
    expect(taskWhileLoading).toBeNull();
    expect(store.note?.audit.version).toBe(7);
  });

  it('that a late answer for a previous note should result into the latest note only', async () => {
    // given
    const { store, lunaNote } = createStore();
    let resolveFirst: (response: TaskResponse) => void = () => undefined;
    let rejectOther: (error: Error) => void = () => undefined;
    lunaNote.GET.mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((_, reject) => (rejectOther = reject)))
      .mockResolvedValueOnce(taskResponse(withVersion(9)));
    const first = store.load('note-1');
    const other = store.load('note-2');
    await store.load('note-3');

    // when
    resolveFirst(taskResponse(withVersion(1)));
    rejectOther(new Error('network down'));
    await Promise.all([first, other]);

    // then
    expect(store.id).toBe('note-3');
    expect(store.note?.audit.version).toBe(9);
    expect(store.loadState).toBe('loaded');
  });

  it('that reloading should result into loading the current note again', async () => {
    // given
    const { store, lunaNote } = await loadedStore();

    // when
    await store.reload();

    // then
    expect(lunaNote.GET).toHaveBeenCalledTimes(2);
  });

  it('that reloading without note should result into no request', async () => {
    // given
    const { store, lunaNote } = createStore();

    // when
    await store.reload();

    // then
    expect(lunaNote.GET).not.toHaveBeenCalled();
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    lunaNote.PUT.mockResolvedValueOnce(taskResponse(withVersion(4)));

    // when
    const saving = store.save(CHANGES);
    const savingWhileRunning = store.saving;
    const saved = await saving;

    // then
    expect(savingWhileRunning).toBe(true);
    expect(saved).toBe(true);
    expect(lunaNote.PUT).toHaveBeenCalledWith('/notes/{id}', {
      params: { path: { id: 'note-1' } },
      body: { ...CHANGES, version: 2 },
    });
    expect(store.note?.audit.version).toBe(4);
    expect(store.saving).toBe(false);
    expect(store.saveError).toBeNull();
  });

  it.each([
    [409, 'conflict'],
    [400, 'invalid'],
    [500, 'failed'],
  ] as const)('that a status %i while saving should result into the save error %s', async (status, error) => {
    // given
    const { store, lunaNote } = await loadedStore();
    lunaNote.PUT.mockResolvedValueOnce(failedResponse(status));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe(error);
    expect(store.note).toEqual(NOTE);
  });

  it('that an unreachable backend while saving should result into a failed save', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    lunaNote.PUT.mockRejectedValueOnce(new Error('network down'));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe('failed');
  });

  it('that saving without loaded note should result into no request', async () => {
    // given
    const { store, lunaNote } = createStore(fakeLunaNoteClient(undefined, null));
    await store.load('unknown');

    // when
    const saved = await store.save(CHANGES);
    const savedWithoutId = await createStore(lunaNote).store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(savedWithoutId).toBe(false);
    expect(lunaNote.PUT).not.toHaveBeenCalled();
  });

  it('that archiving should result into the archived note', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    const completed: NoteDto = { ...withVersion(4), data: { ...NOTE.data, status: 'ARCHIVED' } };
    lunaNote.POST.mockResolvedValueOnce(taskResponse(completed));

    // when
    const completing = store.archive();
    const changingWhileRunning = store.statusChanging;
    await completing;

    // then
    expect(changingWhileRunning).toBe(true);
    expect(lunaNote.POST).toHaveBeenCalledWith('/notes/{id}/archive', { params: { path: { id: 'note-1' } } });
    expect(store.note?.data.status).toBe('ARCHIVED');
    expect(store.statusChanging).toBe(false);
  });

  it('that restoring should result into a request to restore', async () => {
    // given
    const { store, lunaNote } = await loadedStore();

    // when
    await store.restore();

    // then
    expect(lunaNote.POST).toHaveBeenCalledWith('/notes/{id}/restore', { params: { path: { id: 'note-1' } } });
    expect(store.statusError).toBe(false);
  });

  it('that a rejected status change should result into a status error', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    lunaNote.POST.mockResolvedValueOnce(failedResponse(404));

    // when
    await store.archive();

    // then
    expect(store.statusError).toBe(true);
    expect(store.note).toEqual(NOTE);
  });

  it('that an unreachable backend while changing the status should result into a status error', async () => {
    // given
    const { store, lunaNote } = await loadedStore();
    lunaNote.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.restore();

    // then
    expect(store.statusError).toBe(true);
    expect(store.statusChanging).toBe(false);
  });

  it('that changing the status without note should result into no request', async () => {
    // given
    const { store, lunaNote } = createStore();

    // when
    await store.archive();

    // then
    expect(lunaNote.POST).not.toHaveBeenCalled();
  });
});
