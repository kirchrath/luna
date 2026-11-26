import { makeAutoObservable } from 'mobx';

import type { AppointmentDto, AppointmentUpdateDto, LunaAppointmentClient } from '../api';
import type { LoadState } from './TaskStore';

const APPOINTMENT_PATH = '/appointments/{id}';
const CANCEL_PATH = '/appointments/{id}/cancel';
const RESTORE_PATH = '/appointments/{id}/restore';
const HTTP_BAD_REQUEST = 400;
const HTTP_NOT_FOUND = 404;
const HTTP_CONFLICT = 409;

export type AppointmentSaveError = 'conflict' | 'invalid' | 'failed';
export type AppointmentChanges = Omit<AppointmentUpdateDto, 'version'>;

/**
 * One appointment on its detail page: loading, editing, cancelling and restoring.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class AppointmentStore {
  private _id: string | null = null;
  private _appointment: AppointmentDto | null = null;
  private _loadState: LoadState = 'idle';
  private _saving = false;
  private _saveError: AppointmentSaveError | null = null;
  private _statusChanging = false;
  private _statusError = false;

  private readonly client: LunaAppointmentClient;

  constructor(client: LunaAppointmentClient) {
    this.client = client;
    makeAutoObservable<AppointmentStore, 'client'>(this, { client: false });
  }

  get id(): string | null {
    return this._id;
  }

  private set id(id: string | null) {
    this._id = id;
  }

  get appointment(): AppointmentDto | null {
    return this._appointment;
  }

  private set appointment(appointment: AppointmentDto | null) {
    this._appointment = appointment;
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

  get saveError(): AppointmentSaveError | null {
    return this._saveError;
  }

  private set saveError(saveError: AppointmentSaveError | null) {
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
      this.appointment = null;
    }
    this.id = id;
    this.loadState = 'loading';
    this.saveError = null;
    this.statusError = false;
    try {
      const { data, response } = await this.client.GET(APPOINTMENT_PATH, { params: { path: { id } } });
      if (id !== this.id) {
        return;
      }
      if (data === undefined) {
        this.appointment = null;
        this.loadState = response.status === HTTP_NOT_FOUND ? 'notFound' : 'failed';
        return;
      }
      this.appointment = data;
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
  async save(changes: AppointmentChanges): Promise<boolean> {
    const { id, appointment } = this;
    if (id === null || appointment === null) {
      return false;
    }
    this.saving = true;
    this.saveError = null;
    try {
      const { data, response } = await this.client.PUT(APPOINTMENT_PATH, {
        params: { path: { id } },
        body: { ...changes, version: appointment.audit.version },
      });
      if (data === undefined) {
        this.saveError = this.saveErrorOf(response.status);
        return false;
      }
      this.appointment = data;
      return true;
    } catch {
      this.saveError = 'failed';
      return false;
    } finally {
      this.saving = false;
    }
  }

  async cancel(): Promise<void> {
    await this.changeStatus(CANCEL_PATH);
  }

  async restore(): Promise<void> {
    await this.changeStatus(RESTORE_PATH);
  }

  private async changeStatus(path: typeof CANCEL_PATH | typeof RESTORE_PATH): Promise<void> {
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
      this.appointment = data;
    } catch {
      this.statusError = true;
    } finally {
      this.statusChanging = false;
    }
  }

  private saveErrorOf(status: number): AppointmentSaveError {
    if (status === HTTP_CONFLICT) {
      return 'conflict';
    }
    return status === HTTP_BAD_REQUEST ? 'invalid' : 'failed';
  }
}
