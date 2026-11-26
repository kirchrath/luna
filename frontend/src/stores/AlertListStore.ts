import { makeAutoObservable } from 'mobx';

import type { AlertListItemDto, AlertStatusDto, LunaAlertClient, LunaAlertPushConnector, PushConnection } from '../api';
import type { ConnectionState } from './InboundEventStore';

export const ALERT_PAGE_SIZE = 20;
/** Largest page luna-alert returns; a refresh of more visible entries is limited to it. */
export const ALERT_MAX_PAGE_SIZE = 100;
const LIST_PATH = '/alerts';
const ACKNOWLEDGE_PATH = '/alerts/{id}/acknowledge';

export type AlertFilter = 'ACTIVE' | AlertStatusDto | 'ALL';

/** The statuses each filter shows; no status means all alerts. */
export const ALERT_FILTER_STATUSES: Record<AlertFilter, AlertStatusDto[]> = {
  ACTIVE: ['OPEN', 'ACKNOWLEDGED'],
  OPEN: ['OPEN'],
  ACKNOWLEDGED: ['ACKNOWLEDGED'],
  RESOLVED: ['RESOLVED'],
  ALL: [],
};

/**
 * The list of alerts: filter, paging, acknowledging from the list and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class AlertListStore {
  private _items: AlertListItemDto[] = [];
  private _totalCount = 0;
  private _filter: AlertFilter = 'ACTIVE';
  private _loading = false;
  private _loadError = false;
  private _actionError = false;
  private _pendingIds: string[] = [];
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaAlertClient;
  private readonly connectPush: LunaAlertPushConnector;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;
  /** Only the response of the latest request is shown, e.g. after switching the filter quickly. */
  private latestRequest = 0;

  constructor(client: LunaAlertClient, connectPush: LunaAlertPushConnector) {
    this.client = client;
    this.connectPush = connectPush;
    makeAutoObservable<AlertListStore, 'client' | 'connectPush' | 'pushConnection' | 'wasConnected' | 'latestRequest'>(
      this,
      { client: false, connectPush: false, pushConnection: false, wasConnected: false, latestRequest: false },
    );
  }

  get items(): AlertListItemDto[] {
    return this._items;
  }

  private set items(items: AlertListItemDto[]) {
    this._items = items;
  }

  get totalCount(): number {
    return this._totalCount;
  }

  private set totalCount(totalCount: number) {
    this._totalCount = totalCount;
  }

  get filter(): AlertFilter {
    return this._filter;
  }

  private set filter(filter: AlertFilter) {
    this._filter = filter;
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

  get actionError(): boolean {
    return this._actionError;
  }

  private set actionError(actionError: boolean) {
    this._actionError = actionError;
  }

  get pendingIds(): string[] {
    return this._pendingIds;
  }

  private set pendingIds(pendingIds: string[]) {
    this._pendingIds = pendingIds;
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

  isPending(id: string): boolean {
    return this.pendingIds.includes(id);
  }

  async loadInitial(): Promise<void> {
    await this.load(0, ALERT_PAGE_SIZE, 'replace');
  }

  async loadMore(): Promise<void> {
    await this.load(Math.floor(this.items.length / ALERT_PAGE_SIZE), ALERT_PAGE_SIZE, 'append');
  }

  /** Reloads all visible entries, e.g. after a push signal. */
  async refresh(): Promise<void> {
    await this.load(0, Math.min(Math.max(this.items.length, ALERT_PAGE_SIZE), ALERT_MAX_PAGE_SIZE), 'replace');
  }

  async changeFilter(filter: AlertFilter): Promise<void> {
    if (filter === this.filter) {
      return;
    }
    this.filter = filter;
    this.items = [];
    this.totalCount = 0;
    await this.loadInitial();
  }

  /** Acknowledges an open alert from the list. */
  async acknowledge(item: AlertListItemDto): Promise<void> {
    this.pendingIds = [...this.pendingIds, item.id];
    this.actionError = false;
    try {
      const { response } = await this.client.POST(ACKNOWLEDGE_PATH, { params: { path: { id: item.id } } });
      if (!response.ok) {
        this.actionError = true;
        return;
      }
    } catch {
      this.actionError = true;
      return;
    } finally {
      this.pendingIds = this.pendingIds.filter((id) => id !== item.id);
    }
    await this.refresh();
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
    const request = ++this.latestRequest;
    const statuses = ALERT_FILTER_STATUSES[this.filter];
    const query = statuses.length === 0 ? { page, size } : { page, size, status: statuses };
    this.loading = true;
    try {
      const { data } = await this.client.GET(LIST_PATH, { params: { query } });
      if (request !== this.latestRequest) {
        return;
      }
      if (data === undefined) {
        this.loadError = true;
        return;
      }
      this.loadError = false;
      this.totalCount = data.totalCount;
      this.items = mode === 'replace' ? data.items : this.appendNew(data.items);
    } catch {
      if (request === this.latestRequest) {
        this.loadError = true;
      }
    } finally {
      if (request === this.latestRequest) {
        this.loading = false;
      }
    }
  }

  /** Changed alerts shift the pages, so entries already shown are skipped. */
  private appendNew(items: AlertListItemDto[]): AlertListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
