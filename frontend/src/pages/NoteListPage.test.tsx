import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { NoteListDto } from '../api';
import { fakeRootStore, renderApp } from '../test';

const LIST: NoteListDto = {
  totalPages: 2,
  totalCount: 21,
  items: [
    {
      id: 'meeting',
      title: 'Owners meeting',
      summary: 'Roof will be renovated in May',
      status: 'ACTIVE',
      edited: true,
      createdDate: '2026-11-26T13:00:00Z',
    },
    {
      id: 'soup',
      title: 'Pumpkin soup',
      summary: 'Pumpkin, ginger, coconut milk',
      status: 'ARCHIVED',
      edited: false,
      createdDate: '2026-11-24T18:00:00Z',
    },
  ],
};

async function renderPage(noteList: NoteListDto = LIST) {
  const fakes = fakeRootStore(undefined, { noteList });
  const rendered = await renderApp('/notes', { rootStore: fakes.rootStore });
  await screen.findByText('Owners meeting');
  return { ...fakes, ...rendered };
}

function entry(title: string): HTMLElement {
  const item = screen.getByText(title).closest('li');
  if (item === null) {
    throw new Error(`no entry for ${title}`);
  }
  return item;
}

describe('NoteListPage', () => {
  it('that opening the notes should result into the active notes with summary', async () => {
    // when
    const { lunaNote } = await renderPage();

    // then
    expect(screen.getByRole('heading', { level: 1, name: 'Notizen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Aktiv' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('21 aktiv')).toBeInTheDocument();
    expect(lunaNote.GET).toHaveBeenCalledWith('/notes', { params: { query: { page: 0, size: 20, status: 'ACTIVE' } } });
    expect(within(entry('Owners meeting')).getByText('Roof will be renovated in May')).toBeInTheDocument();
    expect(within(entry('Owners meeting')).getByText('· bearbeitet')).toBeInTheDocument();
    expect(within(entry('Owners meeting')).getByRole('link')).toHaveAttribute('href', '/notes/meeting');
    expect(entry('Pumpkin soup')).toHaveClass('is-archived');
    expect(within(entry('Pumpkin soup')).queryByText('· bearbeitet')).toBeNull();
  });

  it('that choosing all notes should result into a list without status filter', async () => {
    // given
    const { lunaNote } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Alle' }));

    // then
    expect(lunaNote.GET).toHaveBeenLastCalledWith('/notes', { params: { query: { page: 0, size: 20 } } });
  });

  it('that loading more should result into the next page', async () => {
    // given
    const { lunaNote } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Mehr laden' }));

    // then
    expect(lunaNote.GET).toHaveBeenCalledTimes(2);
  });

  it('that no notes should result into a hint for the filter', async () => {
    // given
    const fakes = fakeRootStore(undefined, { noteList: { items: [], totalPages: 0, totalCount: 0 } });

    // when
    await renderApp('/notes', { rootStore: fakes.rootStore });

    // then
    expect(
      await screen.findByText('Keine Notizen. Neue Notizen entstehen aus der Schnelleingabe.'),
    ).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaNote.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/notes', { rootStore: fakes.rootStore });
    const message = await screen.findByText('Die Notizen konnten nicht geladen werden.');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaNote.GET).toHaveBeenCalledTimes(2);
    expect(message).not.toBeInTheDocument();
  });

  it('that an interrupted live connection should result into a hint and leaving into a closed connection', async () => {
    // given
    const { notePush, unmount } = await renderPage();

    // when
    act(() => {
      notePush.handlers[0]?.onDisconnected();
    });

    // then
    expect(screen.getByText('Live-Aktualisierung unterbrochen – Luna verbindet sich neu.')).toBeInTheDocument();

    // when
    unmount();

    // then
    await waitFor(() => {
      expect(notePush.close).toHaveBeenCalledTimes(1);
    });
  });
});
