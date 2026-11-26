import { makeAutoObservable } from 'mobx';

import type {
  AppointmentListItemDto,
  AppointmentPeriodDto,
  LunaAppointmentClient,
  LunaAppointmentPushConnector,
  PushConnection,
} from '../api';
import type { ConnectionState } from './InboundEventStore';

export const APPOINTMENT_PAGE_SIZE = 20;
/** Largest page luna-appointment returns; a refresh of more visible entries is limited to it. */
export const APPOINTMENT_MAX_PAGE_SIZE = 100;
const LIST_PATH = '/appointments';

/**
 * The list of appointments: period, paging and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class AppointmentListStore {
  private _items: AppointmentListItemDto[] = [];
  private _totalCount = 0;
  private _filter: AppointmentPeriodDto = 'UPCOMING';
  private _loading = false;
  private _loadError = false;
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaAppointmentClient;
  private readonly connectPush: LunaAppointmentPushConnector;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;
  /** Only the response of the latest request is shown, e.g. after switching the filter quickly. */
  private latestRequest = 0;

  constructor(client: LunaAppointmentClient, connectPush: LunaAppointmentPushConnector) {
    this.client = client;
    this.connectPush = connectPush;
    makeAutoObservable<
      AppointmentListStore,
      'client' | 'connectPush' | 'pushConnection' | 'wasConnected' | 'latestRequest'
    >(this, { client: false, connectPush: false, pushConnection: false, wasConnected: false, latestRequest: false });
  }

  get items(): AppointmentListItemDto[] {
    return this._items;
  }

  private set items(items: AppointmentListItemDto[]) {
    this._items = items;
  }

  get totalCount(): number {
    return this._totalCount;
  }

  private set totalCount(totalCount: number) {
    this._totalCount = totalCount;
  }

  get filter(): AppointmentPeriodDto {
    return this._filter;
  }

  private set filter(filter: AppointmentPeriodDto) {
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
    await this.load(0, APPOINTMENT_PAGE_SIZE, 'replace');
  }

  async loadMore(): Promise<void> {
    await this.load(Math.floor(this.items.length / APPOINTMENT_PAGE_SIZE), APPOINTMENT_PAGE_SIZE, 'append');
  }

  /** Reloads all visible entries, e.g. after a push signal. */
  async refresh(): Promise<void> {
    await this.load(
      0,
      Math.min(Math.max(this.items.length, APPOINTMENT_PAGE_SIZE), APPOINTMENT_MAX_PAGE_SIZE),
      'replace',
    );
  }

  async changeFilter(filter: AppointmentPeriodDto): Promise<void> {
    if (filter === this.filter) {
      return;
    }
    this.filter = filter;
    this.items = [];
    this.totalCount = 0;
    await this.loadInitial();
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
    const query = { page, size, period: this.filter };
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

  /** Changed appointments shift the pages, so entries already shown are skipped. */
  private appendNew(items: AppointmentListItemDto[]): AppointmentListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
