import { makeAutoObservable, reaction } from 'mobx';

import type { LunaTaskClient, LunaTaskPushConnector, PushConnection, TaskListItemDto, TaskStatusDto } from '../api';
import type { ConnectionState } from './InboundEventStore';
import type { LifeAreaStore } from './LifeAreaStore';

export const TASK_PAGE_SIZE = 20;
/** Largest page luna-task returns; a refresh of more visible entries is limited to it. */
export const TASK_MAX_PAGE_SIZE = 100;
const LIST_PATH = '/tasks';
const COMPLETE_PATH = '/tasks/{id}/complete';
const REOPEN_PATH = '/tasks/{id}/reopen';

export const ALL_TASKS = 'ALL';
export type TaskFilter = TaskStatusDto | typeof ALL_TASKS;

/**
 * The list of tasks: filter, paging, completing and reopening from the list and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class TaskListStore {
  private _items: TaskListItemDto[] = [];
  private _totalCount = 0;
  private _filter: TaskFilter = 'OPEN';
  private _loading = false;
  private _loadError = false;
  private _actionError = false;
  private _pendingIds: string[] = [];
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaTaskClient;
  private readonly connectPush: LunaTaskPushConnector;
  private readonly lifeAreaStore: LifeAreaStore;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;
  /** Only the response of the latest request is shown, e.g. after switching the filter quickly. */
  private latestRequest = 0;

  constructor(client: LunaTaskClient, connectPush: LunaTaskPushConnector, lifeAreaStore: LifeAreaStore) {
    this.client = client;
    this.connectPush = connectPush;
    this.lifeAreaStore = lifeAreaStore;
    makeAutoObservable<
      TaskListStore,
      'client' | 'connectPush' | 'lifeAreaStore' | 'pushConnection' | 'wasConnected' | 'latestRequest'
    >(this, {
      client: false,
      connectPush: false,
      lifeAreaStore: false,
      pushConnection: false,
      wasConnected: false,
      latestRequest: false,
    });
    // A list that is shown (connected) follows the chosen area at once; others load it when they are shown next.
    reaction(
      () => this.lifeAreaStore.filter,
      () => {
        if (this.pushConnection !== null) {
          void this.reload();
        }
      },
    );
  }

  get items(): TaskListItemDto[] {
    return this._items;
  }

  private set items(items: TaskListItemDto[]) {
    this._items = items;
  }

  get totalCount(): number {
    return this._totalCount;
  }

  private set totalCount(totalCount: number) {
    this._totalCount = totalCount;
  }

  get filter(): TaskFilter {
    return this._filter;
  }

  private set filter(filter: TaskFilter) {
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
    await this.load(0, TASK_PAGE_SIZE, 'replace');
  }

  async loadMore(): Promise<void> {
    await this.load(Math.floor(this.items.length / TASK_PAGE_SIZE), TASK_PAGE_SIZE, 'append');
  }

  /** Reloads all visible entries, e.g. after a push signal. */
  async refresh(): Promise<void> {
    await this.load(0, Math.min(Math.max(this.items.length, TASK_PAGE_SIZE), TASK_MAX_PAGE_SIZE), 'replace');
  }

  async changeFilter(filter: TaskFilter): Promise<void> {
    if (filter === this.filter) {
      return;
    }
    this.filter = filter;
    await this.reload();
  }

  /** Starts again with the first page, e.g. for another filter or life area. */
  private async reload(): Promise<void> {
    this.items = [];
    this.totalCount = 0;
    await this.loadInitial();
  }

  /** Completes an open task or reopens a done one. */
  async toggle(item: TaskListItemDto): Promise<void> {
    this.pendingIds = [...this.pendingIds, item.id];
    this.actionError = false;
    try {
      const path = item.status === 'OPEN' ? COMPLETE_PATH : REOPEN_PATH;
      const { response } = await this.client.POST(path, { params: { path: { id: item.id } } });
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
    const area = this.lifeAreaStore.query;
    const query = this.filter === ALL_TASKS ? { page, size, ...area } : { page, size, status: this.filter, ...area };
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

  /** Changed tasks shift the pages, so entries already shown are skipped. */
  private appendNew(items: TaskListItemDto[]): TaskListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
