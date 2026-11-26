import { makeAutoObservable } from 'mobx';

import type { LunaNoteClient, NoteDto, NoteUpdateDto } from '../api';
import type { LoadState } from './TaskStore';

const NOTE_PATH = '/notes/{id}';
const ARCHIVE_PATH = '/notes/{id}/archive';
const RESTORE_PATH = '/notes/{id}/restore';
const HTTP_BAD_REQUEST = 400;
const HTTP_NOT_FOUND = 404;
const HTTP_CONFLICT = 409;

export type NoteSaveError = 'conflict' | 'invalid' | 'failed';
export type NoteChanges = Omit<NoteUpdateDto, 'version'>;

/**
 * One note on its detail page: loading, editing, archiving and restoring.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class NoteStore {
  private _id: string | null = null;
  private _note: NoteDto | null = null;
  private _loadState: LoadState = 'idle';
  private _saving = false;
  private _saveError: NoteSaveError | null = null;
  private _statusChanging = false;
  private _statusError = false;

  private readonly client: LunaNoteClient;

  constructor(client: LunaNoteClient) {
    this.client = client;
    makeAutoObservable<NoteStore, 'client'>(this, { client: false });
  }

  get id(): string | null {
    return this._id;
  }

  private set id(id: string | null) {
    this._id = id;
  }

  get note(): NoteDto | null {
    return this._note;
  }

  private set note(note: NoteDto | null) {
    this._note = note;
  }

  get loadState(): LoadState {
    return this._loadState;
  }

  private set loadState(loadState: LoadState) {
    this._loadState = loadState;
  }

  get saving(): boolean {
    return this._saving;
  }

  private set saving(saving: boolean) {
    this._saving = saving;
  }

  get saveError(): NoteSaveError | null {
    return this._saveError;
  }

  private set saveError(saveError: NoteSaveError | null) {
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
      this.note = null;
    }
    this.id = id;
    this.loadState = 'loading';
    this.saveError = null;
    this.statusError = false;
    try {
      const { data, response } = await this.client.GET(NOTE_PATH, { params: { path: { id } } });
      if (id !== this.id) {
        return;
      }
      if (data === undefined) {
        this.note = null;
        this.loadState = response.status === HTTP_NOT_FOUND ? 'notFound' : 'failed';
        return;
      }
      this.note = data;
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
  async save(changes: NoteChanges): Promise<boolean> {
    const { id, note } = this;
    if (id === null || note === null) {
      return false;
    }
    this.saving = true;
    this.saveError = null;
    try {
      const { data, response } = await this.client.PUT(NOTE_PATH, {
        params: { path: { id } },
        body: { ...changes, version: note.audit.version },
      });
      if (data === undefined) {
        this.saveError = this.saveErrorOf(response.status);
        return false;
      }
      this.note = data;
      return true;
    } catch {
      this.saveError = 'failed';
      return false;
    } finally {
      this.saving = false;
    }
  }

  async archive(): Promise<void> {
    await this.changeStatus(ARCHIVE_PATH);
  }

  async restore(): Promise<void> {
    await this.changeStatus(RESTORE_PATH);
  }

  private async changeStatus(path: typeof ARCHIVE_PATH | typeof RESTORE_PATH): Promise<void> {
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
      this.note = data;
    } catch {
      this.statusError = true;
    } finally {
      this.statusChanging = false;
    }
  }

  private saveErrorOf(status: number): NoteSaveError {
    if (status === HTTP_CONFLICT) {
      return 'conflict';
    }
    return status === HTTP_BAD_REQUEST ? 'invalid' : 'failed';
  }
}
