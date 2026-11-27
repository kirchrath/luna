import type {
  LunaAlertClient,
  LunaAlertPushConnector,
  LunaAppointmentClient,
  LunaAppointmentPushConnector,
  LunaCoreClient,
  LunaCorePushConnector,
  LunaNoteClient,
  LunaNotePushConnector,
  LunaTaskClient,
  LunaTaskPushConnector,
} from '../api';
import { AlertListStore } from './AlertListStore';
import { AlertStore } from './AlertStore';
import { AppointmentListStore } from './AppointmentListStore';
import { AppointmentMonthStore } from './AppointmentMonthStore';
import { AppointmentStore } from './AppointmentStore';
import { InboundEventStore } from './InboundEventStore';
import { LifeAreaStore } from './LifeAreaStore';
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
  lunaAppointmentClient: LunaAppointmentClient;
  connectLunaAppointmentPush: LunaAppointmentPushConnector;
}

/**
 * Creates and holds all MobX stores. Every communication with a backend (except OIDC) runs through a store
 * created here.
 */
export class RootStore {
  readonly lifeAreaStore: LifeAreaStore;
  readonly inboundEventStore: InboundEventStore;
  readonly taskListStore: TaskListStore;
  readonly taskStore: TaskStore;
  readonly alertListStore: AlertListStore;
  readonly alertStore: AlertStore;
  readonly noteListStore: NoteListStore;
  readonly noteStore: NoteStore;
  readonly appointmentListStore: AppointmentListStore;
  readonly appointmentMonthStore: AppointmentMonthStore;
  readonly appointmentStore: AppointmentStore;

  constructor({
    lunaCoreClient,
    connectLunaCorePush,
    lunaTaskClient,
    connectLunaTaskPush,
    lunaAlertClient,
    connectLunaAlertPush,
    lunaNoteClient,
    connectLunaNotePush,
    lunaAppointmentClient,
    connectLunaAppointmentPush,
  }: RootStoreDependencies) {
    this.lifeAreaStore = new LifeAreaStore();
    this.inboundEventStore = new InboundEventStore(lunaCoreClient, connectLunaCorePush, this.lifeAreaStore);
    this.taskListStore = new TaskListStore(lunaTaskClient, connectLunaTaskPush, this.lifeAreaStore);
    this.taskStore = new TaskStore(lunaTaskClient);
    this.alertListStore = new AlertListStore(lunaAlertClient, connectLunaAlertPush, this.lifeAreaStore);
    this.alertStore = new AlertStore(lunaAlertClient);
    this.noteListStore = new NoteListStore(lunaNoteClient, connectLunaNotePush, this.lifeAreaStore);
    this.noteStore = new NoteStore(lunaNoteClient);
    this.appointmentListStore = new AppointmentListStore(
      lunaAppointmentClient,
      connectLunaAppointmentPush,
      this.lifeAreaStore,
    );
    this.appointmentMonthStore = new AppointmentMonthStore(
      lunaAppointmentClient,
      connectLunaAppointmentPush,
      this.lifeAreaStore,
    );
    this.appointmentStore = new AppointmentStore(lunaAppointmentClient);
  }
}
