import { createPushConnector, type PushConnector, type TokenSource } from '../shared';
import type { TaskChangedDto } from './types';

/** Topic of the push messages of luna-task (see luna-task/README.md). */
export const TASKS_TOPIC = '/topic/tasks';

export type LunaTaskPushConnector = PushConnector<TaskChangedDto>;

export function createLunaTaskPushConnector(baseUrl: string, tokenSource: TokenSource): LunaTaskPushConnector {
  return createPushConnector<TaskChangedDto>(baseUrl, TASKS_TOPIC, tokenSource);
}
