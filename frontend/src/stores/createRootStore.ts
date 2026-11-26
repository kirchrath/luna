import {
  createLunaAlertClient,
  createLunaAlertPushConnector,
  createLunaCoreClient,
  createLunaCorePushConnector,
  createLunaNoteClient,
  createLunaNotePushConnector,
  createLunaTaskClient,
  createLunaTaskPushConnector,
  type TokenSource,
} from '../api';
import { RootStore } from './RootStore';

export function createRootStore(tokenSource: TokenSource, env: ImportMetaEnv = import.meta.env): RootStore {
  return new RootStore({
    lunaCoreClient: createLunaCoreClient(env.VITE_LUNA_CORE_URL, tokenSource),
    connectLunaCorePush: createLunaCorePushConnector(env.VITE_LUNA_CORE_URL, tokenSource),
    lunaTaskClient: createLunaTaskClient(env.VITE_LUNA_TASK_URL, tokenSource),
    connectLunaTaskPush: createLunaTaskPushConnector(env.VITE_LUNA_TASK_URL, tokenSource),
    lunaAlertClient: createLunaAlertClient(env.VITE_LUNA_ALERT_URL, tokenSource),
    connectLunaAlertPush: createLunaAlertPushConnector(env.VITE_LUNA_ALERT_URL, tokenSource),
    lunaNoteClient: createLunaNoteClient(env.VITE_LUNA_NOTE_URL, tokenSource),
    connectLunaNotePush: createLunaNotePushConnector(env.VITE_LUNA_NOTE_URL, tokenSource),
  });
}
