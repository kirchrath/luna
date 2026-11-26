import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { AlertListDto } from '../api';
import { fakeRootStore, renderApp } from '../test';

const LIST: AlertListDto = {
  totalPages: 2,
  totalCount: 21,
  items: [
    {
      id: 'critical',
      title: 'NAS disk almost full',
      severity: 'CRITICAL',
      affectedSystem: 'nas01',
      status: 'OPEN',
      createdDate: '2026-11-26T16:00:00Z',
    },
    {
      id: 'warning',
      title: 'Mail undeliverable',
      severity: 'WARNING',
      status: 'ACKNOWLEDGED',
      createdDate: '2026-11-26T14:00:00Z',
    },
    {
      id: 'info',
      title: 'Backup done',
      severity: 'INFO',
      affectedSystem: 'backup',
      status: 'OPEN',
      createdDate: '2026-11-25T22:00:00Z',
    },
  ],
};

async function renderPage(alertList: AlertListDto = LIST) {
  const fakes = fakeRootStore(undefined, { alertList });
  const rendered = await renderApp('/alerts', { rootStore: fakes.rootStore });
  await screen.findByText('NAS disk almost full');
  return { ...fakes, ...rendered };
}

function entry(title: string): HTMLElement {
  const item = screen.getByText(title).closest('li');
  if (item === null) {
    throw new Error(`no entry for ${title}`);
  }
  return item;
}

describe('AlertListPage', () => {
  it('that opening the alerts should result into the active alerts with severity and status', async () => {
    // when
    const { lunaAlert } = await renderPage();

    // then
    expect(screen.getByRole('heading', { level: 1, name: 'Warnungen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Aktiv' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('21 aktiv')).toBeInTheDocument();
    expect(lunaAlert.GET).toHaveBeenCalledWith('/alerts', {
      params: { query: { page: 0, size: 20, status: ['OPEN', 'ACKNOWLEDGED'] } },
    });
    expect(within(entry('NAS disk almost full')).getByText('Kritisch').closest('.tag')).toHaveClass('is-critical');
    expect(within(entry('NAS disk almost full')).getByText('nas01 ·')).toBeInTheDocument();
    expect(within(entry('Mail undeliverable')).getByText('Warnung').closest('.tag')).toHaveClass('is-link');
    expect(within(entry('Mail undeliverable')).getByText('Bestätigt')).toBeInTheDocument();
    expect(within(entry('Mail undeliverable')).queryByRole('button')).toBeNull();
    expect(within(entry('Backup done')).getByText('Info')).toBeInTheDocument();
    expect(within(entry('Backup done')).getByRole('link')).toHaveAttribute('href', '/alerts/info');
  });

  it('that acknowledging an alert in the list should result into a request', async () => {
    // given
    const { lunaAlert } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'NAS disk almost full bestätigen' }));

    // then
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/acknowledge', { params: { path: { id: 'critical' } } });
  });

  it('that a failed acknowledgement should result into an error', async () => {
    // given
    const { lunaAlert } = await renderPage();
    lunaAlert.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Backup done bestätigen' }));

    // then
    expect(
      await screen.findByText('Die Warnung konnte nicht geändert werden. Versuche es gleich noch einmal.'),
    ).toBeInTheDocument();
  });

  it('that choosing resolved alerts should result into a list filtered by that status', async () => {
    // given
    const { lunaAlert } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Behoben' }));

    // then
    expect(lunaAlert.GET).toHaveBeenLastCalledWith('/alerts', {
      params: { query: { page: 0, size: 20, status: ['RESOLVED'] } },
    });
  });

  it('that loading more should result into the next page', async () => {
    // given
    const { lunaAlert } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Mehr laden' }));

    // then
    expect(lunaAlert.GET).toHaveBeenCalledTimes(2);
  });

  it('that no alerts should result into a hint for the filter', async () => {
    // given
    const fakes = fakeRootStore(undefined, { alertList: { items: [], totalPages: 0, totalCount: 0 } });

    // when
    await renderApp('/alerts', { rootStore: fakes.rootStore });

    // then
    expect(await screen.findByText('Keine aktiven Warnungen.')).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaAlert.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/alerts', { rootStore: fakes.rootStore });
    const message = await screen.findByText('Die Warnungen konnten nicht geladen werden.');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaAlert.GET).toHaveBeenCalledTimes(2);
    expect(message).not.toBeInTheDocument();
  });

  it('that an interrupted live connection should result into a hint and leaving into a closed connection', async () => {
    // given
    const { alertPush, unmount } = await renderPage();

    // when
    act(() => {
      alertPush.handlers[0]?.onDisconnected();
    });

    // then
    expect(screen.getByText('Live-Aktualisierung unterbrochen – Luna verbindet sich neu.')).toBeInTheDocument();

    // when
    unmount();

    // then
    await waitFor(() => {
      expect(alertPush.close).toHaveBeenCalledTimes(1);
    });
  });
});
