import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { AppointmentListDto, AppointmentListItemDto } from '../api';
import { readPreference, STORAGE_KEYS, writePreference } from '../services';
import { fakeRootStore, renderApp } from '../test';

const NOW = new Date(2026, 10, 26, 12, 0);

function local(month: number, day: number, hours = 0, minutes = 0): string {
  return new Date(2026, month - 1, day, hours, minutes).toISOString();
}

const DENTIST: AppointmentListItemDto = {
  area: 'PERSONAL',
  id: 'dentist',
  title: 'Dentist',
  start: local(11, 26, 14),
  end: local(11, 26, 15),
  allDay: false,
  location: 'Practice at the market',
  status: 'SCHEDULED',
  edited: false,
};
const YOGA: AppointmentListItemDto = {
  area: 'PERSONAL',
  id: 'yoga',
  title: 'Yoga',
  start: local(11, 27, 18),
  allDay: false,
  status: 'CANCELLED',
  edited: true,
};
const TRIP: AppointmentListItemDto = {
  area: 'PERSONAL',
  id: 'trip',
  title: 'Trip to the coast',
  start: local(11, 28),
  end: local(11, 30),
  allDay: true,
  status: 'SCHEDULED',
  edited: false,
};
const CONFERENCE: AppointmentListItemDto = {
  area: 'PERSONAL',
  id: 'conference',
  title: 'Conference',
  start: local(12, 2, 9),
  end: local(12, 4, 17),
  allDay: false,
  status: 'SCHEDULED',
  edited: false,
};

const LIST: AppointmentListDto = { totalPages: 2, totalCount: 21, items: [DENTIST, YOGA, TRIP, CONFERENCE] };

async function renderPage(appointmentList: AppointmentListDto = LIST) {
  const fakes = fakeRootStore(undefined, { appointmentList });
  const rendered = await renderApp('/calendar', { rootStore: fakes.rootStore });
  if (appointmentList.items.length > 0) {
    await screen.findAllByText(/Dentist/);
  }
  return { ...fakes, ...rendered };
}

function day(heading: RegExp | string): HTMLElement {
  const section = screen.getByRole('heading', { level: 2, name: heading }).closest('section');
  if (section === null) {
    throw new Error(`no day ${String(heading)}`);
  }
  return section;
}

function dayHeadings(): string[] {
  return screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent);
}

describe('AppointmentsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'], now: NOW });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('that opening the appointments should result into the upcoming appointments grouped by day', async () => {
    // when
    const { lunaAppointment } = await renderPage();

    // then
    expect(screen.getByRole('heading', { level: 1, name: 'Termine' })).toBeInTheDocument();
    expect(screen.getAllByRole('img', { name: 'Privat' })).not.toHaveLength(0);
    expect(screen.getByRole('button', { name: 'Kommend' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Liste' })).toHaveAttribute('aria-pressed', 'true');
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments', {
      params: { query: { page: 0, size: 20, period: 'UPCOMING' } },
    });
    const today = day(/^Heute, /);
    expect(within(today).getByText('14:00–15:00')).toBeInTheDocument();
    expect(within(today).getByText('Practice at the market')).toBeInTheDocument();
    expect(within(today).getByRole('link')).toHaveAttribute('href', '/calendar/dentist');
    const tomorrow = day(/^Morgen, /);
    expect(within(tomorrow).getByText('18:00')).toBeInTheDocument();
    expect(within(tomorrow).getByText('abgesagt')).toBeInTheDocument();
    expect(within(tomorrow).getByRole('listitem')).toHaveClass('is-cancelled');
    expect(within(day(/28\. November/)).getByText('ganztägig')).toBeInTheDocument();
    expect(within(day(/29\. November/)).getByText('Tag 2 von 3')).toBeInTheDocument();
    expect(within(day(/2\. Dezember/)).getByText('ab 09:00')).toBeInTheDocument();
    expect(within(day(/3\. Dezember/)).getByText('ganztägig')).toBeInTheDocument();
    expect(within(day(/4\. Dezember/)).getByText('bis 17:00')).toBeInTheDocument();
  });

  it('that a running multi-day appointment should result into its days from today on', async () => {
    // given
    const running: AppointmentListItemDto = { ...TRIP, start: local(11, 24), end: local(11, 27) };

    // when
    await renderPage({ totalPages: 1, totalCount: 2, items: [running, DENTIST] });

    // then
    expect(dayHeadings()).toHaveLength(2);
    expect(within(day(/^Heute, /)).getByText('Tag 3 von 4')).toBeInTheDocument();
  });

  it('that choosing past appointments should result into the latest day first', async () => {
    // given
    const { lunaAppointment } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Vergangen' }));

    // then
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments', {
      params: { query: { page: 0, size: 20, period: 'PAST' } },
    });
    await waitFor(() => {
      expect(dayHeadings()[0]).toMatch(/4\. Dezember/);
    });
    expect(dayHeadings().at(-1)).toMatch(/^Heute, /);
  });

  it('that loading more should result into the next page', async () => {
    // given
    const { lunaAppointment } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Mehr laden' }));

    // then
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments', {
      params: { query: { page: 0, size: 20, period: 'UPCOMING' } },
    });
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(2);
  });

  it('that no appointments should result into a hint for the period', async () => {
    // when
    await renderPage({ items: [], totalPages: 0, totalCount: 0 });

    // then
    expect(await screen.findByText('Keine kommenden Termine.')).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/calendar', { rootStore: fakes.rootStore });
    const message = await screen.findByText('Die Termine konnten nicht geladen werden.');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaAppointment.GET).toHaveBeenCalledTimes(2);
    expect(message).not.toBeInTheDocument();
  });

  it('that an interrupted live connection should result into a hint and leaving into a closed connection', async () => {
    // given
    const { appointmentPush, unmount } = await renderPage();

    // when
    act(() => {
      appointmentPush.handlers[0]?.onDisconnected();
    });

    // then
    expect(screen.getByText('Live-Aktualisierung unterbrochen – Luna verbindet sich neu.')).toBeInTheDocument();

    // when
    unmount();

    // then
    await waitFor(() => {
      expect(appointmentPush.close).toHaveBeenCalledTimes(1);
    });
  });

  it('that switching to the month should result into a remembered month view of the current month', async () => {
    // given
    const { lunaAppointment } = await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Monat' }));

    // then
    expect(readPreference(STORAGE_KEYS.appointmentView)).toBe('month');
    const grid = await screen.findByRole('grid', { name: 'November 2026' });
    expect(lunaAppointment.GET).toHaveBeenLastCalledWith('/appointments/range', {
      params: { query: { from: new Date(2026, 10, 1).toISOString(), to: new Date(2026, 11, 1).toISOString() } },
    });
    expect(
      within(grid)
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']);
    expect(await within(grid).findByRole('link', { name: '14:00 Dentist' })).toHaveAttribute(
      'href',
      '/calendar/dentist',
    );
    expect(within(grid).getByRole('link', { name: '18:00 Yoga' })).toHaveClass('is-cancelled');
    expect(within(grid).getAllByRole('link', { name: 'Trip to the coast' })).toHaveLength(3);
    expect(within(grid).getByRole('button', { name: 'Do., 26. November' })).toHaveClass('is-today');
  });

  it('that choosing a day in the month should result into its appointments below the month', async () => {
    // given
    writePreference(STORAGE_KEYS.appointmentView, 'month');
    await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Fr., 27. November' }));

    // then
    expect(screen.getByRole('button', { name: 'Fr., 27. November' })).toHaveAttribute('aria-pressed', 'true');
    const selection = screen.getByRole('region', { name: 'Fr., 27. November' });
    expect(within(selection).getByRole('heading', { name: /^Morgen, / })).toBeInTheDocument();
    expect(within(selection).getByText('Yoga')).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Mi., 4. November' }));

    // then
    expect(screen.getByText('An diesem Tag gibt es keine Termine.')).toBeInTheDocument();
  });

  it('that paging through months should result into other months and back to today', async () => {
    // given
    writePreference(STORAGE_KEYS.appointmentView, 'month');
    await renderPage();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Nächster Monat' }));

    // then
    expect(await screen.findByRole('grid', { name: 'Dezember 2026' })).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Vorheriger Monat' }));
    await userEvent.click(screen.getByRole('button', { name: 'Vorheriger Monat' }));

    // then
    expect(await screen.findByRole('grid', { name: 'Oktober 2026' })).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Heute' }));

    // then
    expect(await screen.findByRole('grid', { name: 'November 2026' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Do., 26. November' })).toBeInTheDocument();
  });

  it('that a failed month and an interrupted connection should result into hints, retry and switching back', async () => {
    // given
    writePreference(STORAGE_KEYS.appointmentView, 'month');
    const fakes = fakeRootStore();
    fakes.lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/calendar', { rootStore: fakes.rootStore });
    await screen.findByText('Die Termine konnten nicht geladen werden.');
    act(() => {
      fakes.appointmentPush.handlers[0]?.onDisconnected();
    });

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(fakes.lunaAppointment.GET).toHaveBeenCalledTimes(2);
    expect(screen.getByText('Live-Aktualisierung unterbrochen – Luna verbindet sich neu.')).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Liste' }));

    // then
    expect(readPreference(STORAGE_KEYS.appointmentView)).toBe('list');
    expect(await screen.findByRole('button', { name: 'Kommend' })).toBeInTheDocument();
  });
});
