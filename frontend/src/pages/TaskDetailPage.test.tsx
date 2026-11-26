import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { TaskDto } from '../api';
import { fakeRootStore, renderApp, TASK } from '../test';

type TaskResponse = Awaited<ReturnType<ReturnType<typeof fakeRootStore>['lunaTask']['PUT']>>;

const DUE = new Date(2026, 10, 30, 10);

const LOADED: TaskDto = { ...TASK, data: { ...TASK.data, dueDate: DUE.toISOString() } };

function response(task: TaskDto | undefined, status = 200): TaskResponse {
  return { data: task, error: undefined, response: new Response(null, { status }) };
}

async function renderPage(task: TaskDto | null = LOADED) {
  const fakes = fakeRootStore(undefined, { task });
  const rendered = await renderApp('/tasks/task-1', { rootStore: fakes.rootStore });
  return { ...fakes, ...rendered };
}

async function renderLoaded(task: TaskDto = LOADED) {
  const rendered = await renderPage(task);
  await screen.findByRole('heading', { level: 1, name: task.data.title });
  return rendered;
}

describe('TaskDetailPage', () => {
  it('that opening a task should result into its data, status and origin', async () => {
    // when
    const { lunaTask } = await renderLoaded();

    // then
    expect(lunaTask.GET).toHaveBeenCalledWith('/tasks/{id}', { params: { path: { id: 'task-1' } } });
    expect(screen.getByText('Aufgaben', { selector: '.task-back span' }).closest('a')).toHaveAttribute(
      'href',
      '/tasks',
    );
    expect(screen.getByText('Offen')).toBeInTheDocument();
    expect(screen.getByText(/Version 3/)).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Pay the electricity bill');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('Customer number 4711');
    expect(screen.getByLabelText('Fällig am')).toHaveValue('2026-11-30');
    expect(screen.getByLabelText('Uhrzeit (optional)')).toHaveValue('10:00');
    expect(screen.getByRole('button', { name: 'Hoch' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('Aus der Schnelleingabe erkannt')).toBeInTheDocument();
    expect(
      screen.getByText('Neue Analyse-Ergebnisse aktualisieren diese Aufgabe, bis du sie bearbeitest.'),
    ).toBeInTheDocument();
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { lunaTask } = await renderLoaded();
    const saved: TaskDto = {
      data: { ...LOADED.data, title: 'Pay the gas bill', priority: 'LOW', edited: true },
      audit: { ...LOADED.audit, version: 4 },
    };
    lunaTask.PUT.mockResolvedValueOnce(response(saved));

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    await userEvent.type(screen.getByLabelText('Titel'), '  Pay the gas bill ');
    await userEvent.click(screen.getByRole('button', { name: 'Niedrig' }));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(lunaTask.PUT).toHaveBeenCalledWith('/tasks/{id}', {
      params: { path: { id: 'task-1' } },
      body: {
        title: 'Pay the gas bill',
        priority: 'LOW',
        description: 'Customer number 4711',
        dueDate: DUE.toISOString(),
        version: 3,
      },
    });
    expect(await screen.findByRole('heading', { level: 1, name: 'Pay the gas bill' })).toBeInTheDocument();
    expect(
      screen.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
    ).toBeInTheDocument();
  });

  it('that removing description and due date should result into an update without them', async () => {
    // given
    const { lunaTask } = await renderLoaded();

    // when
    await userEvent.clear(screen.getByLabelText('Beschreibung'));
    await userEvent.clear(screen.getByLabelText('Fällig am'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.getByLabelText('Uhrzeit (optional)')).toBeDisabled();
    expect(lunaTask.PUT).toHaveBeenCalledWith('/tasks/{id}', {
      params: { path: { id: 'task-1' } },
      body: { title: 'Pay the electricity bill', priority: 'HIGH', version: 3 },
    });
  });

  it('that changing the due date and time should result into the new local time', async () => {
    // given
    const { lunaTask } = await renderLoaded();

    // when
    await userEvent.clear(screen.getByLabelText('Fällig am'));
    await userEvent.type(screen.getByLabelText('Fällig am'), '2026-12-05');
    await userEvent.clear(screen.getByLabelText('Uhrzeit (optional)'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(lunaTask.PUT).toHaveBeenCalledWith(
      '/tasks/{id}',
      expect.objectContaining({
        body: expect.objectContaining({ dueDate: new Date(2026, 11, 5).toISOString() }) as unknown,
      }),
    );
  });

  it('that a task without description and due date should result into empty fields and a time once a date is set', async () => {
    // given
    const { lunaTask } = await renderLoaded({
      ...TASK,
      data: { title: TASK.data.title, priority: 'HIGH', status: 'OPEN', source: 'CORE', edited: false },
    });

    // when
    await userEvent.type(screen.getByLabelText('Fällig am'), '2026-12-05');
    await userEvent.type(screen.getByLabelText('Uhrzeit (optional)'), '08:15');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('');
    expect(lunaTask.PUT).toHaveBeenCalledWith('/tasks/{id}', {
      params: { path: { id: 'task-1' } },
      body: {
        title: TASK.data.title,
        priority: 'HIGH',
        dueDate: new Date(2026, 11, 5, 8, 15).toISOString(),
        version: 3,
      },
    });
  });

  it('that saving without title should result into a hint and no update', async () => {
    // given
    const { lunaTask } = await renderLoaded();

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.getByText('Bitte gib einen Titel ein.')).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveAttribute('aria-invalid', 'true');
    expect(lunaTask.PUT).not.toHaveBeenCalled();

    // when
    await userEvent.type(screen.getByLabelText('Titel'), 'x');

    // then
    expect(screen.queryByText('Bitte gib einen Titel ein.')).toBeNull();
  });

  it('that discarding should result into the stored values', async () => {
    // given
    await renderLoaded();
    await userEvent.type(screen.getByLabelText('Titel'), ' later');
    await userEvent.type(screen.getByLabelText('Beschreibung'), ' and more');
    await userEvent.clear(screen.getByLabelText('Fällig am'));
    await userEvent.click(screen.getByRole('button', { name: 'Normal' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Verwerfen' }));

    // then
    expect(screen.getByLabelText('Titel')).toHaveValue('Pay the electricity bill');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('Customer number 4711');
    expect(screen.getByLabelText('Fällig am')).toHaveValue('2026-11-30');
    expect(screen.getByLabelText('Uhrzeit (optional)')).toHaveValue('10:00');
    expect(screen.getByRole('button', { name: 'Hoch' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('that a conflict should result into a hint that reloads the current version', async () => {
    // given
    const { lunaTask } = await renderLoaded();
    lunaTask.PUT.mockResolvedValueOnce(response(undefined, 409));
    await userEvent.type(screen.getByLabelText('Titel'), ' now');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));
    await screen.findByText('Die Aufgabe wurde inzwischen geändert.');
    lunaTask.GET.mockResolvedValueOnce(
      response({
        ...LOADED,
        data: { ...LOADED.data, title: 'Changed elsewhere' },
        audit: { ...LOADED.audit, version: 5 },
      }),
    );

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Neu laden' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: 'Changed elsewhere' })).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Changed elsewhere');
    expect(screen.queryByText('Die Aufgabe wurde inzwischen geändert.')).toBeNull();
  });

  it.each([
    [400, 'Luna konnte die Änderungen nicht annehmen. Prüfe die Eingaben und versuche es erneut.'],
    [500, 'Speichern fehlgeschlagen. Deine Eingaben bleiben erhalten – versuche es gleich noch einmal.'],
  ])('that a status %i while saving should result into an error and kept input', async (status, message) => {
    // given
    const { lunaTask } = await renderLoaded();
    lunaTask.PUT.mockResolvedValueOnce(response(undefined, status));
    await userEvent.type(screen.getByLabelText('Titel'), ' now');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Pay the electricity bill now');
  });

  it('that completing should result into a done task that can be reopened', async () => {
    // given
    const { lunaTask } = await renderLoaded();
    const completed: TaskDto = {
      data: { ...LOADED.data, status: 'DONE', completedDate: '2026-11-26T14:00:00Z', edited: true },
      audit: { ...LOADED.audit, version: 4 },
    };
    lunaTask.POST.mockResolvedValueOnce(response(completed));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erledigen' }));

    // then
    expect(lunaTask.POST).toHaveBeenCalledWith('/tasks/{id}/complete', { params: { path: { id: 'task-1' } } });
    expect(await screen.findByRole('button', { name: 'Wieder öffnen' })).toBeInTheDocument();
    expect(screen.getByText('Erledigt', { selector: '.tag' })).toBeInTheDocument();
    expect(screen.getByText(/Erledigt 26\.11\.2026/)).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Wieder öffnen' }));

    // then
    expect(lunaTask.POST).toHaveBeenLastCalledWith('/tasks/{id}/reopen', { params: { path: { id: 'task-1' } } });
  });

  it('that a failed status change should result into an error', async () => {
    // given
    const { lunaTask } = await renderLoaded();
    lunaTask.POST.mockResolvedValueOnce(response(undefined, 500));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erledigen' }));

    // then
    expect(
      await screen.findByText('Der Status konnte nicht geändert werden. Versuche es gleich noch einmal.'),
    ).toBeInTheDocument();
  });

  it('that an unknown task should result into a not found hint', async () => {
    // when
    await renderPage(null);

    // then
    expect(await screen.findByRole('heading', { name: 'Aufgabe nicht gefunden' })).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaTask.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/tasks/task-1', { rootStore: fakes.rootStore });
    const alert = await screen.findByRole('alert');
    const message = within(alert).getByText('Die Aufgabe konnte nicht geladen werden.');

    // when
    await userEvent.click(within(alert).getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: TASK.data.title })).toBeInTheDocument();
    expect(message).not.toBeInTheDocument();
  });

  it('that loading should result into a progress bar', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaTask.GET.mockReturnValueOnce(new Promise(() => undefined));

    // when
    await renderApp('/tasks/task-1', { rootStore: fakes.rootStore });

    // then
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});
