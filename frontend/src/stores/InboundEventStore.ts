import { makeAutoObservable } from 'mobx';

import type {
  LunaCoreClient,
  LunaElementTypeDto,
  ManualInboundEventListItemDto,
  PushConnection,
  PushConnector,
} from '../api';

export const PAGE_SIZE = 10;
/** Largest page luna-core returns; a refresh of more visible entries is limited to it. */
export const MAX_PAGE_SIZE = 100;
const HTTP_BAD_REQUEST = 400;
const LIST_PATH = '/inbound-events/manual';

export type ConnectionState = 'idle' | 'connected' | 'disconnected';
export type SubmitError = 'invalid' | 'failed';

/**
 * Manual inputs of the user: submitting, the list of recent inputs and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class InboundEventStore {
  private _items: ManualInboundEventListItemDto[] = [];
  private _totalCount = 0;
  private _loading = false;
  private _loadError = false;
  private _submitting = false;
  private _submitError: SubmitError | null = null;
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaCoreClient;
  private readonly connectPush: PushConnector;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;

  constructor(client: LunaCoreClient, connectPush: PushConnector) {
    this.client = client;
    this.connectPush = connectPush;
    makeAutoObservable<InboundEventStore, 'client' | 'connectPush' | 'pushConnection' | 'wasConnected'>(this, {
      client: false,
      connectPush: false,
      pushConnection: false,
      wasConnected: false,
    });
  }

  get items(): ManualInboundEventListItemDto[] {
    return this._items;
  }

  private set items(items: ManualInboundEventListItemDto[]) {
    this._items = items;
  }

  get totalCount(): number {
    return this._totalCount;
  }

  private set totalCount(totalCount: number) {
    this._totalCount = totalCount;
  }

  get loading(): boolean {
    return this._loading;
  }

  private set loading(loading: boolean) {
    this._loading = loading;
  }

  get loadError(): boolean {
    return this._loadError;
  }

  private set loadError(loadError: boolean) {
    this._loadError = loadError;
  }

  get submitting(): boolean {
    return this._submitting;
  }

  private set submitting(submitting: boolean) {
    this._submitting = submitting;
  }

  get submitError(): SubmitError | null {
    return this._submitError;
  }

  private set submitError(submitError: SubmitError | null) {
    this._submitError = submitError;
  }

  get connection(): ConnectionState {
    return this._connection;
  }

  private set connection(connection: ConnectionState) {
    this._connection = connection;
  }

  get hasMore(): boolean {
    return this.items.length < this.totalCount;
  }

  async loadInitial(): Promise<void> {
    await this.load(0, PAGE_SIZE, 'replace');
  }

  async loadMore(): Promise<void> {
    await this.load(Math.floor(this.items.length / PAGE_SIZE), PAGE_SIZE, 'append');
  }

  /** Reloads all visible entries, e.g. after a push signal. */
  async refresh(): Promise<void> {
    await this.load(0, Math.min(Math.max(this.items.length, PAGE_SIZE), MAX_PAGE_SIZE), 'replace');
  }

  /** Returns whether the input was accepted. */
  async submit(text: string, typeHint: LunaElementTypeDto | undefined): Promise<boolean> {
    this.submitting = true;
    this.submitError = null;
    try {
      const { response } = await this.client.POST(LIST_PATH, {
        body: typeHint === undefined ? { text } : { text, typeHint },
      });
      if (!response.ok) {
        this.submitError = response.status === HTTP_BAD_REQUEST ? 'invalid' : 'failed';
        return false;
      }
    } catch {
      this.submitError = 'failed';
      return false;
    } finally {
      this.submitting = false;
    }
    await this.refresh();
    return true;
  }

  connect(): void {
    this.pushConnection ??= this.connectPush({
      onSignal: () => void this.refresh(),
      onConnected: () => {
        const reconnected = this.wasConnected;
        this.wasConnected = true;
        this.connection = 'connected';
        // Signals sent while the connection was interrupted are lost, so the list is reloaded once.
        if (reconnected) {
          void this.refresh();
        }
      },
      onDisconnected: () => {
        this.connection = 'disconnected';
      },
    });
  }

  async disconnect(): Promise<void> {
    const connection = this.pushConnection;
    this.pushConnection = null;
    this.wasConnected = false;
    this.connection = 'idle';
    await connection?.close();
  }

  private async load(page: number, size: number, mode: 'replace' | 'append'): Promise<void> {
    this.loading = true;
    try {
      const { data } = await this.client.GET(LIST_PATH, { params: { query: { page, size } } });
      if (data === undefined) {
        this.loadError = true;
        return;
      }
      this.loadError = false;
      this.totalCount = data.totalCount;
      this.items = mode === 'replace' ? data.items : this.appendNew(data.items);
    } catch {
      this.loadError = true;
    } finally {
      this.loading = false;
    }
  }

  /** New inputs shift the pages, so entries already shown are skipped. */
  private appendNew(items: ManualInboundEventListItemDto[]): ManualInboundEventListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
