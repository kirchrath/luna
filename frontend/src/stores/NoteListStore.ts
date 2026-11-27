import { makeAutoObservable, reaction } from 'mobx';

import type { LunaNoteClient, LunaNotePushConnector, NoteListItemDto, NoteStatusDto, PushConnection } from '../api';
import type { ConnectionState } from './InboundEventStore';
import type { LifeAreaStore } from './LifeAreaStore';

export const NOTE_PAGE_SIZE = 20;
/** Largest page luna-note returns; a refresh of more visible entries is limited to it. */
export const NOTE_MAX_PAGE_SIZE = 100;
const LIST_PATH = '/notes';

export const ALL_NOTES = 'ALL';
export type NoteFilter = NoteStatusDto | typeof ALL_NOTES;

/**
 * The list of notes: filter, paging and its live updates.
 * State lives in private `_` fields; every change goes through a setter, which MobX turns into an action.
 */
export class NoteListStore {
  private _items: NoteListItemDto[] = [];
  private _totalCount = 0;
  private _filter: NoteFilter = 'ACTIVE';
  private _loading = false;
  private _loadError = false;
  private _connection: ConnectionState = 'idle';

  private readonly client: LunaNoteClient;
  private readonly connectPush: LunaNotePushConnector;
  private readonly lifeAreaStore: LifeAreaStore;
  private pushConnection: PushConnection | null = null;
  private wasConnected = false;
  /** Only the response of the latest request is shown, e.g. after switching the filter quickly. */
  private latestRequest = 0;

  constructor(client: LunaNoteClient, connectPush: LunaNotePushConnector, lifeAreaStore: LifeAreaStore) {
    this.client = client;
    this.connectPush = connectPush;
    this.lifeAreaStore = lifeAreaStore;
    makeAutoObservable<
      NoteListStore,
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

  get items(): NoteListItemDto[] {
    return this._items;
  }

  private set items(items: NoteListItemDto[]) {
    this._items = items;
  }

  get totalCount(): number {
    return this._totalCount;
  }

  private set totalCount(totalCount: number) {
    this._totalCount = totalCount;
  }

  get filter(): NoteFilter {
    return this._filter;
  }

  private set filter(filter: NoteFilter) {
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
    await this.load(0, NOTE_PAGE_SIZE, 'replace');
  }

  async loadMore(): Promise<void> {
    await this.load(Math.floor(this.items.length / NOTE_PAGE_SIZE), NOTE_PAGE_SIZE, 'append');
  }

  /** Reloads all visible entries, e.g. after a push signal. */
  async refresh(): Promise<void> {
    await this.load(0, Math.min(Math.max(this.items.length, NOTE_PAGE_SIZE), NOTE_MAX_PAGE_SIZE), 'replace');
  }

  async changeFilter(filter: NoteFilter): Promise<void> {
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
    const query = this.filter === ALL_NOTES ? { page, size, ...area } : { page, size, status: this.filter, ...area };
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

  /** Changed notes shift the pages, so entries already shown are skipped. */
  private appendNew(items: NoteListItemDto[]): NoteListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
