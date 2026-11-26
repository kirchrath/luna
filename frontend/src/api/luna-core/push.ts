import { createPushConnector, type PushConnector, type TokenSource } from '../shared';
import type { InboundEventChangedDto } from './types';

/** Topic of the push messages of luna-core (see luna-core/README.md). */
export const MANUAL_INBOUND_EVENTS_TOPIC = '/topic/inbound-events/manual';

export type LunaCorePushConnector = PushConnector<InboundEventChangedDto>;

export function createLunaCorePushConnector(baseUrl: string, tokenSource: TokenSource): LunaCorePushConnector {
  return createPushConnector<InboundEventChangedDto>(baseUrl, MANUAL_INBOUND_EVENTS_TOPIC, tokenSource);
}
