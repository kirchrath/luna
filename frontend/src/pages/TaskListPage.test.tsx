import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { TaskListDto } from '../api';
import { fakeRootStore, renderApp } from '../test';

const NOW = new Date(2026, 10, 26, 14, 30);

const LIST: TaskListDto = {
  totalPages: 2,
  totalCount: 21,
  items: [
    {
      area: 'PERSONAL',
      id: 'overdue',
      title: 'Pay the electricity bill',
      dueDate: new Date(2026, 10, 24).toISOString(),
      priority: 'HIGH',
      status: 'OPEN',
      edited: false,
    },
    {
      area: 'PERSONAL',
      id: 'timed',
      title: 'Get the bonus booklet stamped',
      dueDate: new Date(2026, 11, 1, 10).toISOString(),
      priority: 'NORMAL',
      status: 'OPEN',
      edited: true,
    },
    { area: 'PERSONAL', id: 'someday', title: 'Plant the balcony', priority: 'LOW', status: 'OPEN', edited: false },
  ],
};

async function renderPage(taskList: TaskListDto = LIST) {
  const fakes = fakeRootStore(undefined, { taskList });
  const rendered = await renderApp('/tasks', { rootStore: fakes.rootStore });
  await screen.findByText('Pay the electricity bill');
  return { ...fakes, ...rendered };
}

function entry(title: string): HTMLElement {
  const item = screen.getByText(title).closest('li');
  if (item === null) {
    throw new Error(`no entry for ${title}`);
  }
  return item;
}

describe('TaskListPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'], now: NOW });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('that opening the tasks should result into the open tasks with due dates and priorities', async () => {
    // when
    const { lunaTask } = await renderPage();

    // then
    expect(screen.getByRole('heading', { level: 1, name: 'Aufgaben' })).toBeInTheDocument();
    expect(screen.getAllByRole('img', { name: 'Privat' })).not.toHaveLength(0);
    expect(screen.getByRole('button', { name: 'Offen' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('21 offen')).toBeInTheDocument();
    expect(lunaTask.GET).toHaveBeenCalledWith('/tasks', { params: { query: { page: 0, size: 20, status: 'OPEN' } } });
    expect(within(entry('Pay the electricity bill')).getByText('überfällig · 24.11.').closest('.task-due')).toHaveClass(
      'is-overdue',
    );
    expect(within(entry('Pay the electricity bill')).getByText('Hoch')).toBeInTheDocument();
    expect(within(entry('Get the bonus booklet stamped')).getByText('fällig 01.12., 10:00')).toBeInTheDocument();
    expect(within(entry('Get the bonus booklet stamped')).getByText('· bearbeitet')).toBeInTheDocument();
    expect(within(entry('Plant the balcony')).getByText('ohne Fälligkeit')).toBeInTheDocument();
    expect(within(entry('Plant the balcony')).getByRole('link')).toHaveAttribute('href', '/tasks/someday');
  });

  it('that ticking a task should result into completing it', async () => {
    // given
    const { lunaTask } = await renderPage();
    vi.useRealTimers();

    // when
    await userEvent.click(screen.getByRole('checkbox', { name: 'Pay the electricity bill erledigen' }));

    // then
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/complete', { params: { path: { id: 'overdue' } } });
  });

  it('that choosing done tasks should result into the done tasks crossed out', async () => {
    // given
    const { lunaTask } = await renderPage();
    vi.useRealTimers();
    lunaTask.GET.mockResolvedValueOnce({
      data: {
        totalPages: 1,
        totalCount: 1,
        items: [
          {
            area: 'PERSONAL',
            id: 'done',
            title: 'Renew the passport',
            dueDate: new Date(2026, 10, 1).toISOString(),
            priority: 'NORMAL',
            status: 'DONE',
            edited: false,
          },
        ],
      },
      error: undefined,
      response: new Response(),
    });

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erledigt' }));

    // then
    const done = await screen.findByText('Renew the passport');
    expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', {
      params: { query: { page: 0, size: 20, status: 'DONE' } },
    });
    expect(done.closest('li')).toHaveClass('is-done');
    expect(screen.getByText('fällig 01.11.')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Renew the passport wieder öffnen' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });

  it('that loading more should result into the next page', async () => {
    // given
    const { lunaTask } = await renderPage();
    vi.useRealTimers();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Mehr laden' }));

    // then
    expect(lunaTask.GET).toHaveBeenLastCalledWith('/tasks', {
      params: { query: { page: 0, size: 20, status: 'OPEN' } },
    });
  });

  it('that no tasks should result into a hint for the filter', async () => {
    // given
    const fakes = fakeRootStore(undefined, { taskList: { items: [], totalPages: 0, totalCount: 0 } });

    // when
    await renderApp('/tasks', { rootStore: fakes.rootStore });

    // then
    expect(
      await screen.findByText('Keine offenen Aufgaben. Neue Aufgaben entstehen aus der Schnelleingabe.'),
    ).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaTask.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/tasks', { rootStore: fakes.rootStore });
    vi.useRealTimers();

    const message = await screen.findByText('Die Aufgaben konnten nicht geladen werden.');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaTask.GET).toHaveBeenCalledTimes(2);
    expect(message).not.toBeInTheDocument();
  });

  it('that a failed status change should result into an error', async () => {
    // given
    const { lunaTask } = await renderPage();
    vi.useRealTimers();
    lunaTask.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await userEvent.click(screen.getByRole('checkbox', { name: 'Plant the balcony erledigen' }));

    // then
    expect(
      await screen.findByText('Die Aufgabe konnte nicht geändert werden. Versuche es gleich noch einmal.'),
    ).toBeInTheDocument();
  });

  it('that an interrupted live connection should result into a hint and leaving into a closed connection', async () => {
    // given
    const { taskPush, unmount } = await renderPage();

    // when
    act(() => {
      taskPush.handlers[0]?.onDisconnected();
    });

    // then
    expect(screen.getByText('Live-Aktualisierung unterbrochen – Luna verbindet sich neu.')).toBeInTheDocument();

    // when
    unmount();

    // then
    await waitFor(() => {
      expect(taskPush.close).toHaveBeenCalledTimes(1);
    });
  });
});
