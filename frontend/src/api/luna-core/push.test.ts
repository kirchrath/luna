import type { Client, IFrame, IMessage } from '@stomp/stompjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createPushConnector, MANUAL_INBOUND_EVENTS_TOPIC, pushUrl } from './push';

const { FakeClient, clients } = vi.hoisted(() => {
  const created: InstanceType<typeof Fake>[] = [];

  class Fake {
    brokerURL: string | undefined;
    reconnectDelay = 0;
    beforeConnect: (client: Client) => void | Promise<void> = () => undefined;
    onConnect: (frame: IFrame) => void = () => undefined;
    onWebSocketClose: (event: CloseEvent) => void = () => undefined;
    subscriptions: { destination: string; callback: (message: IMessage) => void }[] = [];
    activate = vi.fn();
    deactivate = vi.fn(() => Promise.resolve());

    constructor() {
      created.push(this);
    }

    subscribe(destination: string, callback: (message: IMessage) => void) {
      this.subscriptions.push({ destination, callback });
    }
  }

  return { FakeClient: Fake, clients: created };
});

type FakeClient = InstanceType<typeof FakeClient>;

vi.mock('@stomp/stompjs', () => ({ Client: FakeClient }));

function lastClient(): FakeClient {
  const client = clients.at(-1);
  if (client === undefined) {
    throw new Error('no client created');
  }
  return client;
}

function handlers() {
  return { onSignal: vi.fn(), onConnected: vi.fn(), onDisconnected: vi.fn() };
}

describe('push', () => {
  beforeEach(() => {
    clients.length = 0;
  });

  it('that an absolute http address should result into a websocket address with token', () => {
    // given
    const baseUrl = 'http://localhost:7081';

    // when
    const url = pushUrl(baseUrl, 'token-123');

    // then
    expect(url).toBe('ws://localhost:7081/ws?access_token=token-123');
  });

  it('that a path prefix should result into a websocket address on the origin of the app', () => {
    // given
    const baseUrl = '/api/luna-core';

    // when
    const url = pushUrl(baseUrl, 'token-123');

    // then
    expect(url).toBe(`ws://${window.location.host}/api/luna-core/ws?access_token=token-123`);
  });

  it('that an https address should result into a secure websocket address', () => {
    // given
    const baseUrl = 'https://luna.internal/api/luna-core';

    // when
    const url = pushUrl(baseUrl, 'token-123');

    // then
    expect(url).toBe('wss://luna.internal/api/luna-core/ws?access_token=token-123');
  });

  it('that connecting should result into an activated client that reads the current token before connecting', async () => {
    // given
    const connect = createPushConnector('http://localhost:7081', {
      getUser: () => Promise.resolve({ access_token: 'token-123' }),
    });

    // when
    connect(handlers());
    const client = lastClient();
    await client.beforeConnect(client as unknown as Client);

    // then
    expect(client.activate).toHaveBeenCalledTimes(1);
    expect(client.reconnectDelay).toBe(5000);
    expect(client.brokerURL).toBe('ws://localhost:7081/ws?access_token=token-123');
  });

  it('that connecting without signed in user should result into an empty token', async () => {
    // given
    const connect = createPushConnector('http://localhost:7081', { getUser: () => Promise.resolve(null) });

    // when
    connect(handlers());
    const client = lastClient();
    await client.beforeConnect(client as unknown as Client);

    // then
    expect(client.brokerURL).toBe('ws://localhost:7081/ws?access_token=');
  });

  it('that a received message should result into a signal', () => {
    // given
    const pushHandlers = handlers();
    createPushConnector('http://localhost:7081', { getUser: () => Promise.resolve(null) })(pushHandlers);
    const client = lastClient();
    client.onConnect({} as IFrame);
    const [subscription] = client.subscriptions;

    // when
    subscription?.callback({ body: '{"id":"event-1"}' } as IMessage);

    // then
    expect(pushHandlers.onConnected).toHaveBeenCalledTimes(1);
    expect(subscription?.destination).toBe(MANUAL_INBOUND_EVENTS_TOPIC);
    expect(pushHandlers.onSignal).toHaveBeenCalledWith({ id: 'event-1' });
  });

  it('that a closed websocket should result into a disconnected state', () => {
    // given
    const pushHandlers = handlers();
    createPushConnector('http://localhost:7081', { getUser: () => Promise.resolve(null) })(pushHandlers);

    // when
    lastClient().onWebSocketClose({} as CloseEvent);

    // then
    expect(pushHandlers.onDisconnected).toHaveBeenCalledTimes(1);
  });

  it('that closing the connection should result into a deactivated client', async () => {
    // given
    const connection = createPushConnector('http://localhost:7081', { getUser: () => Promise.resolve(null) })(
      handlers(),
    );

    // when
    await connection.close();

    // then
    expect(lastClient().deactivate).toHaveBeenCalledTimes(1);
  });
});
