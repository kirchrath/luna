import type {
  LunaAlertClient,
  LunaAlertPushConnector,
  LunaCoreClient,
  LunaCorePushConnector,
  LunaNoteClient,
  LunaNotePushConnector,
  LunaTaskClient,
  LunaTaskPushConnector,
} from '../api';
import { AlertListStore } from './AlertListStore';
import { AlertStore } from './AlertStore';
import { InboundEventStore } from './InboundEventStore';
import { NoteListStore } from './NoteListStore';
import { NoteStore } from './NoteStore';
import { TaskListStore } from './TaskListStore';
import { TaskStore } from './TaskStore';

export interface RootStoreDependencies {
  lunaCoreClient: LunaCoreClient;
  connectLunaCorePush: LunaCorePushConnector;
  lunaTaskClient: LunaTaskClient;
  connectLunaTaskPush: LunaTaskPushConnector;
  lunaAlertClient: LunaAlertClient;
  connectLunaAlertPush: LunaAlertPushConnector;
  lunaNoteClient: LunaNoteClient;
  connectLunaNotePush: LunaNotePushConnector;
}

/**
 * Creates and holds all MobX stores. Every communication with a backend (except OIDC) runs through a store
 * created here.
 */
export class RootStore {
  readonly inboundEventStore: InboundEventStore;
  readonly taskListStore: TaskListStore;
  readonly taskStore: TaskStore;
  readonly alertListStore: AlertListStore;
  readonly alertStore: AlertStore;
  readonly noteListStore: NoteListStore;
  readonly noteStore: NoteStore;

  constructor({
    lunaCoreClient,
    connectLunaCorePush,
    lunaTaskClient,
    connectLunaTaskPush,
    lunaAlertClient,
    connectLunaAlertPush,
    lunaNoteClient,
    connectLunaNotePush,
  }: RootStoreDependencies) {
    this.inboundEventStore = new InboundEventStore(lunaCoreClient, connectLunaCorePush);
    this.taskListStore = new TaskListStore(lunaTaskClient, connectLunaTaskPush);
    this.taskStore = new TaskStore(lunaTaskClient);
    this.alertListStore = new AlertListStore(lunaAlertClient, connectLunaAlertPush);
    this.alertStore = new AlertStore(lunaAlertClient);
    this.noteListStore = new NoteListStore(lunaNoteClient, connectLunaNotePush);
    this.noteStore = new NoteStore(lunaNoteClient);
  }
}
