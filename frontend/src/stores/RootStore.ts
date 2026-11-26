import type { LunaCoreClient, LunaCorePushConnector, LunaTaskClient, LunaTaskPushConnector } from '../api';
import { InboundEventStore } from './InboundEventStore';
import { TaskListStore } from './TaskListStore';
import { TaskStore } from './TaskStore';

export interface RootStoreDependencies {
  lunaCoreClient: LunaCoreClient;
  connectLunaCorePush: LunaCorePushConnector;
  lunaTaskClient: LunaTaskClient;
  connectLunaTaskPush: LunaTaskPushConnector;
}

/**
 * Creates and holds all MobX stores. Every communication with a backend (except OIDC) runs through a store
 * created here.
 */
export class RootStore {
  readonly inboundEventStore: InboundEventStore;
  readonly taskListStore: TaskListStore;
  readonly taskStore: TaskStore;

  constructor({ lunaCoreClient, connectLunaCorePush, lunaTaskClient, connectLunaTaskPush }: RootStoreDependencies) {
    this.inboundEventStore = new InboundEventStore(lunaCoreClient, connectLunaCorePush);
    this.taskListStore = new TaskListStore(lunaTaskClient, connectLunaTaskPush);
    this.taskStore = new TaskStore(lunaTaskClient);
  }
}
