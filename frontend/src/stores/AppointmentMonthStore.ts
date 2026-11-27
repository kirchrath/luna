import { makeAutoObservable, reaction } from 'mobx';

import type {
  AppointmentListItemDto,
  LunaAppointmentClient,
  LunaAppointmentPushConnector,
  PushConnection,
} from '../api';
import type { ConnectionState } from './InboundEventStore';
import type { LifeAreaStore } from './LifeAreaStore';

const RANGE_PATH = '/appointments/range';
const FIRST_DAY = 1;

/**
 * One month of the calendar: the appointments overlapping it, the chosen day and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class AppointmentMonthStore {
  /** First day of the shown month at 00:00 local time. */
  private _month: Date;
  private _selectedDay: Date | null = null;
  private _items: AppointmentListItemDto[] = [];
  private _loading = false;
  private _loadError = false;
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaAppointmentClient;
  private readonly connectPush: LunaAppointmentPushConnector;
  private readonly lifeAreaStore: LifeAreaStore;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;
  /** Only the response of the latest request is shown, e.g. after paging through months quickly. */
  private latestRequest = 0;

  constructor(
    client: LunaAppointmentClient,
    connectPush: LunaAppointmentPushConnector,
    lifeAreaStore: LifeAreaStore,
    today: Date = new Date(),
  ) {
    this.client = client;
    this.connectPush = connectPush;
    this.lifeAreaStore = lifeAreaStore;
    this._month = new Date(today.getFullYear(), today.getMonth(), FIRST_DAY);
    makeAutoObservable<
      AppointmentMonthStore,
      'client' | 'connectPush' | 'lifeAreaStore' | 'pushConnection' | 'wasConnected' | 'latestRequest'
    >(this, {
      client: false,
      connectPush: false,
      lifeAreaStore: false,
      pushConnection: false,
      wasConnected: false,
      latestRequest: false,
    });
    // A list that is shown (connected) follows the shown areas at once; others load them when they are shown next.
    reaction(
      () => this.lifeAreaStore.query.area?.join(),
      () => {
        if (this.pushConnection !== null) {
          void this.load();
        }
      },
    );
  }

  get month(): Date {
    return this._month;
  }

  private set month(month: Date) {
    this._month = month;
  }

  get selectedDay(): Date | null {
    return this._selectedDay;
  }

  private set selectedDay(selectedDay: Date | null) {
    this._selectedDay = selectedDay;
  }

  get items(): AppointmentListItemDto[] {
    return this._items;
  }

  private set items(items: AppointmentListItemDto[]) {
    this._items = items;
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

  async previousMonth(): Promise<void> {
    await this.showMonth(new Date(this.month.getFullYear(), this.month.getMonth() - 1, FIRST_DAY));
  }

  async nextMonth(): Promise<void> {
    await this.showMonth(new Date(this.month.getFullYear(), this.month.getMonth() + 1, FIRST_DAY));
  }

  /** Shows the month of today and selects today. */
  async showToday(today: Date = new Date()): Promise<void> {
    this.selectedDay = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    await this.showMonth(new Date(today.getFullYear(), today.getMonth(), FIRST_DAY), false);
  }

  selectDay(day: Date): void {
    this.selectedDay = day;
  }

  async load(): Promise<void> {
    const request = ++this.latestRequest;
    const from = this.month;
    const to = new Date(from.getFullYear(), from.getMonth() + 1, FIRST_DAY);
    this.loading = true;
    try {
      const { data } = await this.client.GET(RANGE_PATH, {
        params: { query: { from: from.toISOString(), to: to.toISOString(), ...this.lifeAreaStore.query } },
      });
      if (request !== this.latestRequest) {
        return;
      }
      this.loadError = data === undefined;
      this.items = data?.items ?? [];
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

  connect(): void {
    this.pushConnection ??= this.connectPush({
      onSignal: () => void this.load(),
      onConnected: () => {
        const reconnected = this.wasConnected;
        this.wasConnected = true;
        this.connection = 'connected';
        // Signals sent while the connection was interrupted are lost, so the month is reloaded once.
        if (reconnected) {
          void this.load();
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

  private async showMonth(month: Date, clearSelection = true): Promise<void> {
    this.month = month;
    if (clearSelection) {
      this.selectedDay = null;
    }
    this.items = [];
    await this.load();
  }
}
