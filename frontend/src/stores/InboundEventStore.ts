import { makeAutoObservable, runInAction } from 'mobx';

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

/** Manual inputs of the user: submitting, the list of recent inputs and its live updates. */
export class InboundEventStore {
  items: ManualInboundEventListItemDto[] = [];
  totalCount = 0;
  loading = false;
  loadError = false;
  submitting = false;
  submitError: SubmitError | null = null;
  connection: ConnectionState = 'idle';

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
        runInAction(() => {
          this.submitError = response.status === HTTP_BAD_REQUEST ? 'invalid' : 'failed';
        });
        return false;
      }
    } catch {
      runInAction(() => {
        this.submitError = 'failed';
      });
      return false;
    } finally {
      runInAction(() => {
        this.submitting = false;
      });
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
        this.setConnection('connected');
        // Signals sent while the connection was interrupted are lost, so the list is reloaded once.
        if (reconnected) {
          void this.refresh();
        }
      },
      onDisconnected: () => {
        this.setConnection('disconnected');
      },
    });
  }

  async disconnect(): Promise<void> {
    const connection = this.pushConnection;
    this.pushConnection = null;
    this.wasConnected = false;
    this.setConnection('idle');
    await connection?.close();
  }

  private setConnection(connection: ConnectionState): void {
    this.connection = connection;
  }

  private async load(page: number, size: number, mode: 'replace' | 'append'): Promise<void> {
    this.loading = true;
    try {
      const { data } = await this.client.GET(LIST_PATH, { params: { query: { page, size } } });
      runInAction(() => {
        if (data === undefined) {
          this.loadError = true;
          return;
        }
        this.loadError = false;
        this.totalCount = data.totalCount;
        this.items = mode === 'replace' ? data.items : this.appendNew(data.items);
      });
    } catch {
      runInAction(() => {
        this.loadError = true;
      });
    } finally {
      runInAction(() => {
        this.loading = false;
      });
    }
  }

  /** New inputs shift the pages, so entries already shown are skipped. */
  private appendNew(items: ManualInboundEventListItemDto[]): ManualInboundEventListItemDto[] {
    const known = new Set(this.items.map((item) => item.id));
    return [...this.items, ...items.filter((item) => !known.has(item.id))];
  }
}
