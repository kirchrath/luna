import { makeAutoObservable } from 'mobx';

import type { AlertDto, LunaAlertClient } from '../api';
import type { LoadState } from './TaskStore';

const ALERT_PATH = '/alerts/{id}';
const ACKNOWLEDGE_PATH = '/alerts/{id}/acknowledge';
const RESOLVE_PATH = '/alerts/{id}/resolve';
const REOPEN_PATH = '/alerts/{id}/reopen';
const HTTP_NOT_FOUND = 404;

/**
 * One alert on its detail page: loading, acknowledging, resolving and reopening.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class AlertStore {
  private _id: string | null = null;
  private _alert: AlertDto | null = null;
  private _loadState: LoadState = 'idle';
  private _statusChanging = false;
  private _statusError = false;

  private readonly client: LunaAlertClient;

  constructor(client: LunaAlertClient) {
    this.client = client;
    makeAutoObservable<AlertStore, 'client'>(this, { client: false });
  }

  get id(): string | null {
    return this._id;
  }

  private set id(id: string | null) {
    this._id = id;
  }

  get alert(): AlertDto | null {
    return this._alert;
  }

  private set alert(alert: AlertDto | null) {
    this._alert = alert;
  }

  get loadState(): LoadState {
    return this._loadState;
  }

  private set loadState(loadState: LoadState) {
    this._loadState = loadState;
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
      this.alert = null;
    }
    this.id = id;
    this.loadState = 'loading';
    this.statusError = false;
    try {
      const { data, response } = await this.client.GET(ALERT_PATH, { params: { path: { id } } });
      if (id !== this.id) {
        return;
      }
      if (data === undefined) {
        this.alert = null;
        this.loadState = response.status === HTTP_NOT_FOUND ? 'notFound' : 'failed';
        return;
      }
      this.alert = data;
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

  async acknowledge(): Promise<void> {
    await this.changeStatus(ACKNOWLEDGE_PATH);
  }

  async resolve(): Promise<void> {
    await this.changeStatus(RESOLVE_PATH);
  }

  async reopen(): Promise<void> {
    await this.changeStatus(REOPEN_PATH);
  }

  private async changeStatus(path: typeof ACKNOWLEDGE_PATH | typeof RESOLVE_PATH | typeof REOPEN_PATH): Promise<void> {
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
      this.alert = data;
    } catch {
      this.statusError = true;
    } finally {
      this.statusChanging = false;
    }
  }
}
