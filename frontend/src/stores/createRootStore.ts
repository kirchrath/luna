import { createLunaCoreClient, createPushConnector, type TokenSource } from '../api';
import { RootStore } from './RootStore';

export function createRootStore(tokenSource: TokenSource, env: ImportMetaEnv = import.meta.env): RootStore {
  return new RootStore({
    lunaCoreClient: createLunaCoreClient(env.VITE_LUNA_CORE_URL, tokenSource),
    connectLunaCorePush: createPushConnector(env.VITE_LUNA_CORE_URL, tokenSource),
  });
}
