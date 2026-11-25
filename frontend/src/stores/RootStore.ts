import type { LunaCoreClient, PushConnector } from '../api';
import { InboundEventStore } from './InboundEventStore';

export interface RootStoreDependencies {
  lunaCoreClient: LunaCoreClient;
  connectLunaCorePush: PushConnector;
}

/**
 * Creates and holds all MobX stores. Every communication with a backend (except OIDC) runs through a store
 * created here.
 */
export class RootStore {
  readonly inboundEventStore: InboundEventStore;

  constructor({ lunaCoreClient, connectLunaCorePush }: RootStoreDependencies) {
    this.inboundEventStore = new InboundEventStore(lunaCoreClient, connectLunaCorePush);
  }
}
