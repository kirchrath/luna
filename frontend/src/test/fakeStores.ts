import { vi } from 'vitest';

import type {
  AlertDto,
  AlertListDto,
  LunaAlertClient,
  LunaCoreClient,
  LunaNoteClient,
  LunaTaskClient,
  ManualInboundEventListDto,
  NoteDto,
  NoteListDto,
  PushConnector,
  PushHandlers,
  TaskDto,
  TaskListDto,
} from '../api';
import { RootStore } from '../stores';

const EMPTY_LIST: ManualInboundEventListDto = { items: [], totalPages: 0, totalCount: 0 };
const EMPTY_PAGE = { items: [], totalPages: 0, totalCount: 0 };

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

export const ALERT: AlertDto = {
  data: {
    title: 'NAS disk almost full',
    message: 'Volume 1 is 94 % full',
    severity: 'CRITICAL',
    affectedSystem: 'nas01',
    status: 'OPEN',
    source: 'CORE',
    sourceId: 'core-alert-1',
  },
  audit: { createdDate: '2026-11-26T16:05:00Z', lastModifiedDate: '2026-11-26T16:05:00Z', version: 0 },
};

export const NOTE: NoteDto = {
  data: {
    title: 'Owners meeting',
    summary: 'Roof will be renovated in May',
    status: 'ACTIVE',
    source: 'CORE',
    sourceId: 'core-note-1',
    edited: false,
  },
  audit: { createdDate: '2026-11-26T13:02:00Z', lastModifiedDate: '2026-11-26T13:20:00Z', version: 2 },
};

function respond<T>(data: T | undefined, status = 200) {
  return Promise.resolve({ data, error: undefined, response: new Response(null, { status }) });
}

export function fakeLunaCoreClient(list: ManualInboundEventListDto = EMPTY_LIST) {
  const GET = vi.fn(() => respond(list));
  const POST = vi.fn(() => respond({ id: 'new-id' }, 202));
  return { GET, POST, client: { GET, POST } as unknown as LunaCoreClient };
}

/** Answers list requests with the list and all other requests with the element (or 404 if it is null). */
function fakeElementClient<List, Element>(listPath: string, list: List, element: Element | null) {
  const answer = () => (element === null ? respond<Element>(undefined, 404) : respond(element));
  const GET = vi.fn((path: string) => (path === listPath ? respond<List | Element>(list) : answer()));
  const POST = vi.fn(answer);
  const PUT = vi.fn(answer);
  return { GET, POST, PUT, client: { GET, POST, PUT } };
}

export function fakeLunaTaskClient(list: TaskListDto = EMPTY_PAGE, task: TaskDto | null = TASK) {
  const fake = fakeElementClient<TaskListDto, TaskDto>('/tasks', list, task);
  return { ...fake, client: fake.client as unknown as LunaTaskClient };
}

export function fakeLunaAlertClient(list: AlertListDto = EMPTY_PAGE, alert: AlertDto | null = ALERT) {
  const fake = fakeElementClient<AlertListDto, AlertDto>('/alerts', list, alert);
  return { ...fake, client: fake.client as unknown as LunaAlertClient };
}

export function fakeLunaNoteClient(list: NoteListDto = EMPTY_PAGE, note: NoteDto | null = NOTE) {
  const fake = fakeElementClient<NoteListDto, NoteDto>('/notes', list, note);
  return { ...fake, client: fake.client as unknown as LunaNoteClient };
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
  alertList?: AlertListDto;
  alert?: AlertDto | null;
  noteList?: NoteListDto;
  note?: NoteDto | null;
}

export function fakeRootStore(
  list?: ManualInboundEventListDto,
  { taskList, task, alertList, alert, noteList, note }: FakeRootStoreOptions = {},
) {
  const lunaCore = fakeLunaCoreClient(list);
  const push = fakePushConnector();
  const lunaTask = fakeLunaTaskClient(taskList, task);
  const taskPush = fakePushConnector();
  const lunaAlert = fakeLunaAlertClient(alertList, alert);
  const alertPush = fakePushConnector();
  const lunaNote = fakeLunaNoteClient(noteList, note);
  const notePush = fakePushConnector();
  return {
    rootStore: new RootStore({
      lunaCoreClient: lunaCore.client,
      connectLunaCorePush: push.connect,
      lunaTaskClient: lunaTask.client,
      connectLunaTaskPush: taskPush.connect,
      lunaAlertClient: lunaAlert.client,
      connectLunaAlertPush: alertPush.connect,
      lunaNoteClient: lunaNote.client,
      connectLunaNotePush: notePush.connect,
    }),
    lunaCore,
    push,
    lunaTask,
    taskPush,
    lunaAlert,
    alertPush,
    lunaNote,
    notePush,
  };
}
