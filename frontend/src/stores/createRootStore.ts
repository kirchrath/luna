import {
  createLunaCoreClient,
  createLunaCorePushConnector,
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
  });
}
