import { Client } from '@stomp/stompjs';

import type { InboundEventChangedDto } from './types';
import type { TokenSource } from './tokenSource';

/** Endpoint and topic of the push messages of luna-core (see luna-core/README.md). */
export const PUSH_ENDPOINT = '/ws';
export const MANUAL_INBOUND_EVENTS_TOPIC = '/topic/inbound-events/manual';
const ACCESS_TOKEN_PARAMETER = 'access_token';
const RECONNECT_DELAY_MS = 5000;
const HTTP_SCHEME = /^http/;
const WEB_SOCKET_SCHEME = 'ws';

export interface PushHandlers {
  onSignal: (signal: InboundEventChangedDto) => void;
  onConnected: () => void;
  onDisconnected: () => void;
}

export interface PushConnection {
  close: () => Promise<void>;
}

export type PushConnector = (handlers: PushHandlers) => PushConnection;

export function pushUrl(baseUrl: string, token: string): string {
  const url = new URL(`${baseUrl}${PUSH_ENDPOINT}`, window.location.href);
  // http becomes ws, https becomes wss.
  url.protocol = url.protocol.replace(HTTP_SCHEME, WEB_SOCKET_SCHEME);
  url.searchParams.set(ACCESS_TOKEN_PARAMETER, token);
  return url.toString();
}

export function createPushConnector(baseUrl: string, tokenSource: TokenSource): PushConnector {
  return ({ onSignal, onConnected, onDisconnected }) => {
    const client = new Client();
    client.reconnectDelay = RECONNECT_DELAY_MS;
    // The token is read again before every (re)connect, so that a renewed token is used.
    client.beforeConnect = async (stompClient) => {
      const user = await tokenSource.getUser();
      stompClient.brokerURL = pushUrl(baseUrl, user?.access_token ?? '');
    };
    client.onConnect = () => {
      client.subscribe(MANUAL_INBOUND_EVENTS_TOPIC, (message) => {
        onSignal(JSON.parse(message.body) as InboundEventChangedDto);
      });
      onConnected();
    };
    client.onWebSocketClose = () => {
      onDisconnected();
    };
    client.activate();
    return { close: () => client.deactivate() };
  };
}
