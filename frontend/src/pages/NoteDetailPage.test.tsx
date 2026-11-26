import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { NoteDto } from '../api';
import { fakeRootStore, NOTE, renderApp } from '../test';

type NoteResponse = Awaited<ReturnType<ReturnType<typeof fakeRootStore>['lunaNote']['PUT']>>;

function response(note: NoteDto | undefined, status = 200): NoteResponse {
  return { data: note, error: undefined, response: new Response(null, { status }) };
}

async function renderLoaded(note: NoteDto = NOTE) {
  const fakes = fakeRootStore(undefined, { note });
  const rendered = await renderApp('/notes/note-1', { rootStore: fakes.rootStore });
  await screen.findByRole('heading', { level: 1, name: note.data.title });
  return { ...fakes, ...rendered };
}

describe('NoteDetailPage', () => {
  it('that opening a note should result into its data, status and origin', async () => {
    // when
    const { lunaNote } = await renderLoaded();

    // then
    expect(lunaNote.GET).toHaveBeenCalledWith('/notes/{id}', { params: { path: { id: 'note-1' } } });
    expect(screen.getByText('Notizen', { selector: '.element-back span' }).closest('a')).toHaveAttribute(
      'href',
      '/notes',
    );
    expect(screen.getByText('Aktiv', { selector: '.tag' })).toBeInTheDocument();
    expect(screen.getByText(/Version 2/)).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Owners meeting');
    expect(screen.getByLabelText('Zusammenfassung')).toHaveValue('Roof will be renovated in May');
    expect(
      screen.getByText('Neue Analyse-Ergebnisse aktualisieren diese Notiz, bis du sie bearbeitest.'),
    ).toBeInTheDocument();
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { lunaNote } = await renderLoaded();
    const saved: NoteDto = {
      data: { ...NOTE.data, title: 'Owners meeting 2026', edited: true },
      audit: { ...NOTE.audit, version: 3 },
    };
    lunaNote.PUT.mockResolvedValueOnce(response(saved));

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    await userEvent.type(screen.getByLabelText('Titel'), ' Owners meeting 2026 ');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(lunaNote.PUT).toHaveBeenCalledWith('/notes/{id}', {
      params: { path: { id: 'note-1' } },
      body: { title: 'Owners meeting 2026', summary: 'Roof will be renovated in May', version: 2 },
    });
    expect(await screen.findByRole('heading', { level: 1, name: 'Owners meeting 2026' })).toBeInTheDocument();
    expect(
      screen.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
    ).toBeInTheDocument();
  });

  it('that saving without title and summary should result into hints and no update', async () => {
    // given
    const { lunaNote } = await renderLoaded();

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    await userEvent.clear(screen.getByLabelText('Zusammenfassung'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.getByText('Bitte gib einen Titel ein.')).toBeInTheDocument();
    expect(screen.getByText('Bitte gib eine Zusammenfassung ein.')).toBeInTheDocument();
    expect(lunaNote.PUT).not.toHaveBeenCalled();

    // when
    await userEvent.type(screen.getByLabelText('Titel'), 'x');
    await userEvent.type(screen.getByLabelText('Zusammenfassung'), 'y');

    // then
    expect(screen.queryByText('Bitte gib einen Titel ein.')).toBeNull();
    expect(screen.queryByText('Bitte gib eine Zusammenfassung ein.')).toBeNull();
  });

  it('that discarding should result into the stored values', async () => {
    // given
    await renderLoaded();
    await userEvent.type(screen.getByLabelText('Titel'), ' later');
    await userEvent.clear(screen.getByLabelText('Zusammenfassung'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Verwerfen' }));

    // then
    expect(screen.getByLabelText('Titel')).toHaveValue('Owners meeting');
    expect(screen.getByLabelText('Zusammenfassung')).toHaveValue('Roof will be renovated in May');
    expect(screen.queryByText('Bitte gib eine Zusammenfassung ein.')).toBeNull();
  });

  it('that a conflict should result into a hint that reloads the current version', async () => {
    // given
    const { lunaNote } = await renderLoaded();
    lunaNote.PUT.mockResolvedValueOnce(response(undefined, 409));
    await userEvent.type(screen.getByLabelText('Titel'), ' now');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));
    await screen.findByText('Die Notiz wurde inzwischen geändert.');
    lunaNote.GET.mockResolvedValueOnce(
      response({ ...NOTE, data: { ...NOTE.data, title: 'Changed elsewhere' }, audit: { ...NOTE.audit, version: 5 } }),
    );

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Neu laden' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: 'Changed elsewhere' })).toBeInTheDocument();
    expect(screen.getByLabelText('Titel')).toHaveValue('Changed elsewhere');
  });

  it.each([
    [400, 'Luna konnte die Änderungen nicht annehmen. Prüfe die Eingaben und versuche es erneut.'],
    [500, 'Speichern fehlgeschlagen. Deine Eingaben bleiben erhalten – versuche es gleich noch einmal.'],
  ])('that a status %i while saving should result into an error', async (status, message) => {
    // given
    const { lunaNote } = await renderLoaded();
    lunaNote.PUT.mockResolvedValueOnce(response(undefined, status));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(await screen.findByText(message)).toBeInTheDocument();
  });

  it('that archiving should result into an archived note that can be restored', async () => {
    // given
    const { lunaNote } = await renderLoaded();
    const archived: NoteDto = {
      data: { ...NOTE.data, status: 'ARCHIVED', archivedDate: '2026-11-26T14:00:00Z', edited: true },
      audit: { ...NOTE.audit, version: 3 },
    };
    lunaNote.POST.mockResolvedValueOnce(response(archived));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Archivieren' }));

    // then
    expect(lunaNote.POST).toHaveBeenCalledWith('/notes/{id}/archive', { params: { path: { id: 'note-1' } } });
    expect(await screen.findByRole('button', { name: 'Wiederherstellen' })).toBeInTheDocument();
    expect(screen.getByText(/Archiviert 26\.11\.2026/)).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Wiederherstellen' }));

    // then
    expect(lunaNote.POST).toHaveBeenLastCalledWith('/notes/{id}/restore', { params: { path: { id: 'note-1' } } });
  });

  it('that a failed status change should result into an error', async () => {
    // given
    const { lunaNote } = await renderLoaded();
    lunaNote.POST.mockResolvedValueOnce(response(undefined, 500));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Archivieren' }));

    // then
    expect(
      await screen.findByText('Der Status konnte nicht geändert werden. Versuche es gleich noch einmal.'),
    ).toBeInTheDocument();
  });

  it('that an unknown note should result into a not found hint', async () => {
    // given
    const fakes = fakeRootStore(undefined, { note: null });

    // when
    await renderApp('/notes/unknown', { rootStore: fakes.rootStore });

    // then
    expect(await screen.findByRole('heading', { name: 'Notiz nicht gefunden' })).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaNote.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/notes/note-1', { rootStore: fakes.rootStore });
    const alert = await screen.findByRole('alert');
    const message = within(alert).getByText('Die Notiz konnte nicht geladen werden.');

    // when
    await userEvent.click(within(alert).getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: NOTE.data.title })).toBeInTheDocument();
    expect(message).not.toBeInTheDocument();
  });

  it('that loading should result into a progress bar', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaNote.GET.mockReturnValueOnce(new Promise(() => undefined));

    // when
    await renderApp('/notes/note-1', { rootStore: fakes.rootStore });

    // then
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});
