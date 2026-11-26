import { createPushConnector, type PushConnector, type TokenSource } from '../shared';
import type { AlertChangedDto } from './types';

/** Topic of the push messages of luna-alert (see luna-alert/README.md). */
export const ALERTS_TOPIC = '/topic/alerts';

export type LunaAlertPushConnector = PushConnector<AlertChangedDto>;

export function createLunaAlertPushConnector(baseUrl: string, tokenSource: TokenSource): LunaAlertPushConnector {
  return createPushConnector<AlertChangedDto>(baseUrl, ALERTS_TOPIC, tokenSource);
}
