import { describe, expect, it } from 'vitest';

import { createRootStore } from './createRootStore';
import { AlertListStore } from './AlertListStore';
import { AlertStore } from './AlertStore';
import { InboundEventStore } from './InboundEventStore';
import { NoteListStore } from './NoteListStore';
import { NoteStore } from './NoteStore';
import { TaskListStore } from './TaskListStore';
import { TaskStore } from './TaskStore';

describe('createRootStore', () => {
  it('that creating the root store should result into all stores', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    const rootStore = createRootStore(tokenSource, {
      VITE_LUNA_CORE_URL: 'http://localhost:7081',
      VITE_LUNA_TASK_URL: 'http://localhost:7082',
      VITE_LUNA_ALERT_URL: 'http://localhost:7083',
      VITE_LUNA_NOTE_URL: 'http://localhost:7084',
    } as ImportMetaEnv);

    // then
    expect(rootStore.inboundEventStore).toBeInstanceOf(InboundEventStore);
    expect(rootStore.taskListStore).toBeInstanceOf(TaskListStore);
    expect(rootStore.taskStore).toBeInstanceOf(TaskStore);
    expect(rootStore.alertListStore).toBeInstanceOf(AlertListStore);
    expect(rootStore.alertStore).toBeInstanceOf(AlertStore);
    expect(rootStore.noteListStore).toBeInstanceOf(NoteListStore);
    expect(rootStore.noteStore).toBeInstanceOf(NoteStore);
  });

  it('that creating the root store without environment should result into the vite environment', () => {
    // given
    const tokenSource = { getUser: () => Promise.resolve(null) };

    // when
    const rootStore = createRootStore(tokenSource);

    // then
    expect(rootStore.inboundEventStore).toBeInstanceOf(InboundEventStore);
  });
});
