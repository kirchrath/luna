import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { AlertDto } from '../api';
import { ALERT, fakeRootStore, renderApp } from '../test';

type AlertResponse = Awaited<ReturnType<ReturnType<typeof fakeRootStore>['lunaAlert']['POST']>>;

function response(alert: AlertDto | undefined, status = 200): AlertResponse {
  return { data: alert, error: undefined, response: new Response(null, { status }) };
}

function withStatus(status: AlertDto['data']['status'], dates: Partial<AlertDto['data']> = {}): AlertDto {
  return { ...ALERT, data: { ...ALERT.data, status, ...dates }, audit: { ...ALERT.audit, version: 1 } };
}

async function renderLoaded(alert: AlertDto = ALERT) {
  const fakes = fakeRootStore(undefined, { alert });
  const rendered = await renderApp('/alerts/alert-1', { rootStore: fakes.rootStore });
  await screen.findByRole('heading', { level: 1, name: alert.data.title });
  return { ...fakes, ...rendered };
}

describe('AlertDetailPage', () => {
  it('that opening an open alert should result into its message, severity and the actions for open alerts', async () => {
    // when
    const { lunaAlert } = await renderLoaded();

    // then
    expect(lunaAlert.GET).toHaveBeenCalledWith('/alerts/{id}', { params: { path: { id: 'alert-1' } } });
    expect(screen.getByText('Warnungen', { selector: '.element-back span' }).closest('a')).toHaveAttribute(
      'href',
      '/alerts',
    );
    expect(screen.getByText('Kritisch')).toBeInTheDocument();
    expect(screen.getByText('Offen', { selector: '.tag' })).toBeInTheDocument();
    expect(screen.getByText('Volume 1 is 94 % full')).toBeInTheDocument();
    expect(screen.getByText('nas01')).toBeInTheDocument();
    expect(screen.getByText('Aus der Schnelleingabe erkannt')).toBeInTheDocument();
    expect(screen.getByText('Bereich: Privat')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bestätigen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Als behoben' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Wieder öffnen' })).toBeNull();
  });

  it('that acknowledging should result into an acknowledged alert that can be resolved or reopened', async () => {
    // given
    const { lunaAlert } = await renderLoaded();
    lunaAlert.POST.mockResolvedValueOnce(
      response(withStatus('ACKNOWLEDGED', { acknowledgedDate: '2026-11-26T16:10:00Z' })),
    );

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Bestätigen' }));

    // then
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/acknowledge', { params: { path: { id: 'alert-1' } } });
    expect(await screen.findByRole('button', { name: 'Wieder öffnen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Als behoben' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bestätigen' })).toBeNull();
    expect(screen.getByText(/Bestätigt 26\.11\.2026/)).toBeInTheDocument();
  });

  it('that a resolved alert should result into reopening as the only action', async () => {
    // given
    const resolved = withStatus('RESOLVED', {
      acknowledgedDate: '2026-11-26T16:10:00Z',
      resolvedDate: '2026-11-26T16:20:00Z',
    });
    const { lunaAlert } = await renderLoaded(resolved);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Wieder öffnen' }));

    // then
    expect(screen.queryByRole('button', { name: 'Als behoben' })).toBeNull();
    expect(screen.getByText(/Behoben 26\.11\.2026/)).toBeInTheDocument();
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/reopen', { params: { path: { id: 'alert-1' } } });
  });

  it('that resolving should result into a request to resolve', async () => {
    // given
    const { lunaAlert } = await renderLoaded();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Als behoben' }));

    // then
    expect(lunaAlert.POST).toHaveBeenCalledWith('/alerts/{id}/resolve', { params: { path: { id: 'alert-1' } } });
  });

  it('that an alert without affected system should result into no system section', async () => {
    // given
    const data: AlertDto['data'] = {
      area: 'PERSONAL',
      title: ALERT.data.title,
      message: ALERT.data.message,
      severity: 'INFO',
      status: 'OPEN',
      source: 'CORE',
    };

    // when
    await renderLoaded({ ...ALERT, data });

    // then
    expect(screen.queryByText('Betroffenes System')).toBeNull();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('that a failed status change should result into an error', async () => {
    // given
    const { lunaAlert } = await renderLoaded();
    lunaAlert.POST.mockResolvedValueOnce(response(undefined, 409));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Bestätigen' }));

    // then
    expect(
      await screen.findByText('Der Status konnte nicht geändert werden. Lade die Seite neu und versuche es erneut.'),
    ).toBeInTheDocument();
  });

  it('that an unknown alert should result into a not found hint', async () => {
    // given
    const fakes = fakeRootStore(undefined, { alert: null });

    // when
    await renderApp('/alerts/unknown', { rootStore: fakes.rootStore });

    // then
    expect(await screen.findByRole('heading', { name: 'Warnung nicht gefunden' })).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaAlert.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/alerts/alert-1', { rootStore: fakes.rootStore });
    const alert = await screen.findByRole('alert');
    const message = within(alert).getByText('Die Warnung konnte nicht geladen werden.');

    // when
    await userEvent.click(within(alert).getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: ALERT.data.title })).toBeInTheDocument();
    expect(message).not.toBeInTheDocument();
  });

  it('that loading should result into a progress bar', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaAlert.GET.mockReturnValueOnce(new Promise(() => undefined));

    // when
    await renderApp('/alerts/alert-1', { rootStore: fakes.rootStore });

    // then
    expect(screen.getByRole('progressbar', { name: 'Warnung wird geladen' })).toBeInTheDocument();
  });
});
