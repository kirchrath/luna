import { makeAutoObservable } from 'mobx';

import type { LunaTaskClient, TaskDto, TaskUpdateDto } from '../api';

const TASK_PATH = '/tasks/{id}';
const COMPLETE_PATH = '/tasks/{id}/complete';
const REOPEN_PATH = '/tasks/{id}/reopen';
const HTTP_BAD_REQUEST = 400;
const HTTP_NOT_FOUND = 404;
const HTTP_CONFLICT = 409;

export type TaskLoadState = 'idle' | 'loading' | 'loaded' | 'notFound' | 'failed';
export type TaskSaveError = 'conflict' | 'invalid' | 'failed';
export type TaskChanges = Omit<TaskUpdateDto, 'version'>;

/**
 * One task on its detail page: loading, editing, completing and reopening.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class TaskStore {
  private _id: string | null = null;
  private _task: TaskDto | null = null;
  private _loadState: TaskLoadState = 'idle';
  private _saving = false;
  private _saveError: TaskSaveError | null = null;
  private _statusChanging = false;
  private _statusError = false;

  private readonly client: LunaTaskClient;

  constructor(client: LunaTaskClient) {
    this.client = client;
    makeAutoObservable<TaskStore, 'client'>(this, { client: false });
  }

  get id(): string | null {
    return this._id;
  }

  private set id(id: string | null) {
    this._id = id;
  }

  get task(): TaskDto | null {
    return this._task;
  }

  private set task(task: TaskDto | null) {
    this._task = task;
  }

  get loadState(): TaskLoadState {
    return this._loadState;
  }

  private set loadState(loadState: TaskLoadState) {
    this._loadState = loadState;
  }

  get saving(): boolean {
    return this._saving;
  }

  private set saving(saving: boolean) {
    this._saving = saving;
  }

  get saveError(): TaskSaveError | null {
    return this._saveError;
  }

  private set saveError(saveError: TaskSaveError | null) {
    this._saveError = saveError;
  }

  get statusChanging(): boolean {
    return this._statusChanging;
  }

  private set statusChanging(statusChanging: boolean) {
    this._statusChanging = statusChanging;
  }

  get statusError(): boolean {
    return this._statusError;
  }

  private set statusError(statusError: boolean) {
    this._statusError = statusError;
  }

  async load(id: string): Promise<void> {
    if (id !== this.id) {
      this.task = null;
    }
    this.id = id;
    this.loadState = 'loading';
    this.saveError = null;
    this.statusError = false;
    try {
      const { data, response } = await this.client.GET(TASK_PATH, { params: { path: { id } } });
      if (id !== this.id) {
        return;
      }
      if (data === undefined) {
        this.task = null;
        this.loadState = response.status === HTTP_NOT_FOUND ? 'notFound' : 'failed';
        return;
      }
      this.task = data;
      this.loadState = 'loaded';
    } catch {
      if (id === this.id) {
        this.loadState = 'failed';
      }
    }
  }

  /** Loads the current state again, e.g. after a conflict. */
  async reload(): Promise<void> {
    if (this.id !== null) {
      await this.load(this.id);
    }
  }

  /** Saves the changes against the loaded version; returns whether they were saved. */
  async save(changes: TaskChanges): Promise<boolean> {
    const { id, task } = this;
    if (id === null || task === null) {
      return false;
    }
    this.saving = true;
    this.saveError = null;
    try {
      const { data, response } = await this.client.PUT(TASK_PATH, {
        params: { path: { id } },
        body: { ...changes, version: task.audit.version },
      });
      if (data === undefined) {
        this.saveError = this.saveErrorOf(response.status);
        return false;
      }
      this.task = data;
      return true;
    } catch {
      this.saveError = 'failed';
      return false;
    } finally {
      this.saving = false;
    }
  }

  async complete(): Promise<void> {
    await this.changeStatus(COMPLETE_PATH);
  }

  async reopen(): Promise<void> {
    await this.changeStatus(REOPEN_PATH);
  }

  private async changeStatus(path: typeof COMPLETE_PATH | typeof REOPEN_PATH): Promise<void> {
    const { id } = this;
    if (id === null) {
      return;
    }
    this.statusChanging = true;
    this.statusError = false;
    try {
      const { data } = await this.client.POST(path, { params: { path: { id } } });
      if (data === undefined) {
        this.statusError = true;
        return;
      }
      this.task = data;
    } catch {
      this.statusError = true;
    } finally {
      this.statusChanging = false;
    }
  }

  private saveErrorOf(status: number): TaskSaveError {
    if (status === HTTP_CONFLICT) {
      return 'conflict';
    }
    return status === HTTP_BAD_REQUEST ? 'invalid' : 'failed';
  }
}
