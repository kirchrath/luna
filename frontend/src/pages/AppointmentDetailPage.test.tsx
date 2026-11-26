import { fireEvent, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { AppointmentDto } from '../api';
import { APPOINTMENT, fakeRootStore, renderApp } from '../test';

type AppointmentResponse = Awaited<ReturnType<ReturnType<typeof fakeRootStore>['lunaAppointment']['PUT']>>;

function local(month: number, day: number, hours = 0, minutes = 0): string {
  return new Date(2026, month - 1, day, hours, minutes).toISOString();
}

const DENTIST: AppointmentDto = {
  ...APPOINTMENT,
  data: { ...APPOINTMENT.data, start: local(12, 1, 9), end: local(12, 1, 10) },
};

function response(appointment: AppointmentDto | undefined, status = 200): AppointmentResponse {
  return { data: appointment, error: undefined, response: new Response(null, { status }) };
}

type Data = AppointmentDto['data'];

/** The dentist appointment with changed fields and without the omitted optional ones. */
function withData(data: Partial<Data>, omitted: readonly ('end' | 'location' | 'description')[] = []): AppointmentDto {
  const merged = Object.fromEntries(
    Object.entries({ ...DENTIST.data, ...data }).filter(([key]) => !omitted.some((field) => field === key)),
  ) as Data;
  return { ...DENTIST, data: merged };
}

async function renderLoaded(appointment: AppointmentDto = DENTIST) {
  const fakes = fakeRootStore(undefined, { appointment });
  const rendered = await renderApp('/calendar/appointment-1', { rootStore: fakes.rootStore });
  await screen.findByRole('heading', { level: 1, name: appointment.data.title });
  return { ...fakes, ...rendered };
}

function metaLine(): string | null {
  return document.querySelector('.element-meta-line span:last-child')?.textContent ?? null;
}

describe('AppointmentDetailPage', () => {
  it('that opening an appointment should result into its data, status and origin', async () => {
    // when
    const { lunaAppointment } = await renderLoaded();

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments/{id}', {
      params: { path: { id: 'appointment-1' } },
    });
    expect(screen.getByText('Termine', { selector: '.element-back span' }).closest('a')).toHaveAttribute(
      'href',
      '/calendar',
    );
    expect(screen.getByText('Geplant', { selector: '.tag' })).toBeInTheDocument();
    expect(metaLine()).toBe('Di., 1. Dezember, 09:00–10:00 · Version 1');
    expect(screen.getByLabelText('Titel')).toHaveValue('Dentist Dr. Weber');
    expect(screen.getByLabelText('Ganztägig')).not.toBeChecked();
    expect(screen.getByLabelText('Beginn')).toHaveValue('2026-12-01');
    expect(screen.getByLabelText('Uhrzeit Beginn')).toHaveValue('09:00');
    expect(screen.getByLabelText('Ende (optional)')).toHaveValue('2026-12-01');
    expect(screen.getByLabelText('Uhrzeit Ende')).toHaveValue('10:00');
    expect(screen.getByLabelText('Ort')).toHaveValue('Practice at the market');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('Bring the bonus booklet');
    expect(
      screen.getByText('Neue Analyse-Ergebnisse aktualisieren diesen Termin, bis du ihn bearbeitest.'),
    ).toBeInTheDocument();
  });

  it.each([
    [withData({}, ['end']), 'Di., 1. Dezember, 09:00 · Version 1'],
    [withData({ end: local(12, 3, 17) }), 'Di., 1. Dezember, 09:00 – Do., 3. Dezember, 17:00 · Version 1'],
    [withData({ allDay: true, start: local(12, 1) }, ['end']), 'Di., 1. Dezember, ganztägig · Version 1'],
    [
      withData({ allDay: true, start: local(12, 1), end: local(12, 3) }),
      'Di., 1. Dezember – Do., 3. Dezember, ganztägig · Version 1',
    ],
  ])('that an appointment should result into its time span %#', async (appointment, expected) => {
    // when
    await renderLoaded(appointment);

    // then
    expect(metaLine()).toBe(expected);
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();
    const saved: AppointmentDto = {
      data: { ...DENTIST.data, title: 'Dentist check-up', edited: true },
      audit: { ...DENTIST.audit, version: 2 },
    };
    lunaAppointment.PUT.mockResolvedValueOnce(response(saved));

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    await userEvent.type(screen.getByLabelText('Titel'), ' Dentist check-up ');
    fireEvent.change(screen.getByLabelText('Uhrzeit Beginn'), { target: { value: '08:30' } });
    fireEvent.change(screen.getByLabelText('Uhrzeit Ende'), { target: { value: '09:30' } });
    await userEvent.clear(screen.getByLabelText('Ort'));
    await userEvent.clear(screen.getByLabelText('Beschreibung'));
    await userEvent.type(screen.getByLabelText('Beschreibung'), 'Bonus booklet');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(lunaAppointment.PUT).toHaveBeenCalledWith('/appointments/{id}', {
      params: { path: { id: 'appointment-1' } },
      body: {
        title: 'Dentist check-up',
        start: local(12, 1, 8, 30),
        end: local(12, 1, 9, 30),
        allDay: false,
        description: 'Bonus booklet',
        version: 1,
      },
    });
    expect(await screen.findByRole('heading', { level: 1, name: 'Dentist check-up' })).toBeInTheDocument();
    expect(
      screen.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
    ).toBeInTheDocument();
  });

  it('that saving an all-day appointment without end should result into days without times', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();

    // when
    await userEvent.click(screen.getByLabelText('Ganztägig'));
    fireEvent.change(screen.getByLabelText('Letzter Tag (optional)'), { target: { value: '' } });
    await userEvent.clear(screen.getByLabelText('Ort'));
    await userEvent.type(screen.getByLabelText('Ort'), ' Home ');
    await userEvent.clear(screen.getByLabelText('Beschreibung'));
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.queryByLabelText('Uhrzeit Beginn')).toBeNull();
    expect(screen.queryByLabelText('Uhrzeit Ende')).toBeNull();
    expect(lunaAppointment.PUT).toHaveBeenCalledWith('/appointments/{id}', {
      params: { path: { id: 'appointment-1' } },
      body: { title: 'Dentist Dr. Weber', start: local(12, 1), allDay: true, location: 'Home', version: 1 },
    });
  });

  it('that saving without title and with an end before the start should result into hints and no update', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();

    // when
    await userEvent.clear(screen.getByLabelText('Titel'));
    fireEvent.change(screen.getByLabelText('Ende (optional)'), { target: { value: '2026-11-30' } });
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(screen.getByText('Bitte gib einen Titel ein.')).toBeInTheDocument();
    expect(
      screen.getByText('Bitte gib einen Beginn an; das Ende darf nicht vor dem Beginn liegen.'),
    ).toBeInTheDocument();
    expect(lunaAppointment.PUT).not.toHaveBeenCalled();

    // when
    await userEvent.type(screen.getByLabelText('Titel'), 'x');
    fireEvent.change(screen.getByLabelText('Ende (optional)'), { target: { value: '2026-12-01' } });

    // then
    expect(screen.queryByText('Bitte gib einen Titel ein.')).toBeNull();
    expect(screen.queryByText('Bitte gib einen Beginn an; das Ende darf nicht vor dem Beginn liegen.')).toBeNull();
  });

  it.each(['Beginn', 'Uhrzeit Beginn', 'Uhrzeit Ende'])(
    'that changing %s after invalid times should result into no hint',
    async (field) => {
      // given
      await renderLoaded();
      fireEvent.change(screen.getByLabelText('Beginn'), { target: { value: '' } });
      await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));
      const hint = screen.getByText('Bitte gib einen Beginn an; das Ende darf nicht vor dem Beginn liegen.');

      // when
      fireEvent.change(screen.getByLabelText(field), {
        target: { value: field === 'Beginn' ? '2026-12-01' : '11:00' },
      });

      // then
      expect(hint).not.toBeInTheDocument();
    },
  );

  it('that discarding should result into the stored values', async () => {
    // given
    await renderLoaded();
    await userEvent.type(screen.getByLabelText('Titel'), ' later');
    await userEvent.click(screen.getByLabelText('Ganztägig'));
    fireEvent.change(screen.getByLabelText('Beginn'), { target: { value: '' } });
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Verwerfen' }));

    // then
    expect(screen.getByLabelText('Titel')).toHaveValue('Dentist Dr. Weber');
    expect(screen.getByLabelText('Ganztägig')).not.toBeChecked();
    expect(screen.getByLabelText('Beginn')).toHaveValue('2026-12-01');
    expect(screen.queryByText('Bitte gib einen Beginn an; das Ende darf nicht vor dem Beginn liegen.')).toBeNull();
  });

  it('that an appointment without optional fields should result into empty inputs', async () => {
    // when
    await renderLoaded(withData({}, ['end', 'location', 'description']));

    // then
    expect(screen.getByLabelText('Ende (optional)')).toHaveValue('');
    expect(screen.getByLabelText('Ort')).toHaveValue('');
    expect(screen.getByLabelText('Beschreibung')).toHaveValue('');
  });

  it('that a conflict should result into a hint that reloads the current version', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();
    lunaAppointment.PUT.mockResolvedValueOnce(response(undefined, 409));
    await userEvent.type(screen.getByLabelText('Titel'), ' now');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));
    await screen.findByText('Der Termin wurde inzwischen geändert.');
    lunaAppointment.GET.mockResolvedValueOnce(
      response({ ...withData({ title: 'Changed elsewhere' }), audit: { ...DENTIST.audit, version: 5 } }),
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
    const { lunaAppointment } = await renderLoaded();
    lunaAppointment.PUT.mockResolvedValueOnce(response(undefined, status));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    // then
    expect(await screen.findByText(message)).toBeInTheDocument();
  });

  it('that cancelling should result into a cancelled appointment that can be restored', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();
    const cancelled: AppointmentDto = {
      data: { ...DENTIST.data, status: 'CANCELLED', cancelledDate: '2026-11-26T14:00:00Z', edited: true },
      audit: { ...DENTIST.audit, version: 2 },
    };
    lunaAppointment.POST.mockResolvedValueOnce(response(cancelled));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absagen' }));

    // then
    expect(lunaAppointment.POST).toHaveBeenCalledWith('/appointments/{id}/cancel', {
      params: { path: { id: 'appointment-1' } },
    });
    expect(await screen.findByRole('button', { name: 'Wiederherstellen' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toHaveClass('is-cancelled');
    expect(screen.getByText('Abgesagt', { selector: '.tag' })).toBeInTheDocument();
    expect(screen.getByText(/Abgesagt 26\.11\.2026/)).toBeInTheDocument();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Wiederherstellen' }));

    // then
    expect(lunaAppointment.POST).toHaveBeenLastCalledWith('/appointments/{id}/restore', {
      params: { path: { id: 'appointment-1' } },
    });
  });

  it('that a failed status change should result into an error', async () => {
    // given
    const { lunaAppointment } = await renderLoaded();
    lunaAppointment.POST.mockResolvedValueOnce(response(undefined, 500));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Absagen' }));

    // then
    expect(
      await screen.findByText('Der Status konnte nicht geändert werden. Versuche es gleich noch einmal.'),
    ).toBeInTheDocument();
  });

  it('that an unknown appointment should result into a not found hint', async () => {
    // given
    const fakes = fakeRootStore(undefined, { appointment: null });

    // when
    await renderApp('/calendar/unknown', { rootStore: fakes.rootStore });

    // then
    expect(await screen.findByRole('heading', { name: 'Termin nicht gefunden' })).toBeInTheDocument();
  });

  it('that a failed load should result into an error with retry', async () => {
    // given
    const fakes = fakeRootStore(undefined, { appointment: DENTIST });
    fakes.lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));
    await renderApp('/calendar/appointment-1', { rootStore: fakes.rootStore });
    const alert = await screen.findByRole('alert');
    const message = within(alert).getByText('Der Termin konnte nicht geladen werden.');

    // when
    await userEvent.click(within(alert).getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(await screen.findByRole('heading', { level: 1, name: DENTIST.data.title })).toBeInTheDocument();
    expect(message).not.toBeInTheDocument();
  });

  it('that loading should result into a progress bar', async () => {
    // given
    const fakes = fakeRootStore();
    fakes.lunaAppointment.GET.mockReturnValueOnce(new Promise(() => undefined));

    // when
    await renderApp('/calendar/appointment-1', { rootStore: fakes.rootStore });

    // then
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});
