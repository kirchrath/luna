import { vi } from 'vitest';

import type {
  LunaCoreClient,
  LunaTaskClient,
  ManualInboundEventListDto,
  PushConnector,
  PushHandlers,
  TaskDto,
  TaskListDto,
} from '../api';
import { RootStore } from '../stores';

const EMPTY_LIST: ManualInboundEventListDto = { items: [], totalPages: 0, totalCount: 0 };
const EMPTY_TASK_LIST: TaskListDto = { items: [], totalPages: 0, totalCount: 0 };
const TASK_LIST_PATH = '/tasks';

export const TASK: TaskDto = {
  data: {
    title: 'Pay the electricity bill',
    description: 'Customer number 4711',
    dueDate: '2026-11-30T09:00:00Z',
    priority: 'HIGH',
    status: 'OPEN',
    source: 'CORE',
    sourceId: 'core-task-1',
    edited: false,
  },
  audit: { createdDate: '2026-11-24T08:12:00Z', lastModifiedDate: '2026-11-26T13:03:00Z', version: 3 },
};

function respond<T>(data: T | undefined, status = 200) {
  return Promise.resolve({ data, error: undefined, response: new Response(null, { status }) });
}

export function fakeLunaCoreClient(list: ManualInboundEventListDto = EMPTY_LIST) {
  const GET = vi.fn(() => respond(list));
  const POST = vi.fn(() => respond({ id: 'new-id' }, 202));
  return { GET, POST, client: { GET, POST } as unknown as LunaCoreClient };
}

/** Answers list requests with the list and all other requests with the task (or 404 if it is null). */
export function fakeLunaTaskClient(list: TaskListDto = EMPTY_TASK_LIST, task: TaskDto | null = TASK) {
  const answer = () => (task === null ? respond<TaskDto>(undefined, 404) : respond(task));
  const GET = vi.fn((path: string) => (path === TASK_LIST_PATH ? respond<TaskListDto | TaskDto>(list) : answer()));
  const POST = vi.fn(answer);
  const PUT = vi.fn(answer);
  return { GET, POST, PUT, client: { GET, POST, PUT } as unknown as LunaTaskClient };
}

export function fakePushConnector<Signal = { id: string }>() {
  const handlers: PushHandlers<Signal>[] = [];
  const close = vi.fn(() => Promise.resolve());
  const connect = vi.fn<PushConnector<Signal>>((pushHandlers) => {
    handlers.push(pushHandlers);
    return { close };
  });
  return { connect, close, handlers };
}

interface FakeRootStoreOptions {
  taskList?: TaskListDto;
  task?: TaskDto | null;
}

export function fakeRootStore(list?: ManualInboundEventListDto, { taskList, task }: FakeRootStoreOptions = {}) {
  const lunaCore = fakeLunaCoreClient(list);
  const push = fakePushConnector();
  const lunaTask = fakeLunaTaskClient(taskList, task);
  const taskPush = fakePushConnector();
  return {
    rootStore: new RootStore({
      lunaCoreClient: lunaCore.client,
      connectLunaCorePush: push.connect,
      lunaTaskClient: lunaTask.client,
      connectLunaTaskPush: taskPush.connect,
    }),
    lunaCore,
    push,
    lunaTask,
    taskPush,
  };
}
