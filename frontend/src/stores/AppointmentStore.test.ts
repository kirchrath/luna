import { describe, expect, it } from 'vitest';

import type { AppointmentDto } from '../api';
import { fakeLunaAppointmentClient, APPOINTMENT } from '../test';
import { AppointmentStore } from './AppointmentStore';

type AppointmentResponse = Awaited<ReturnType<ReturnType<typeof fakeLunaAppointmentClient>['PUT']>>;

const CHANGES = { title: 'Dentist', start: '2026-12-02T09:00:00Z', allDay: false } as const;

function appointmentResponse(appointment: AppointmentDto): AppointmentResponse {
  return { data: appointment, error: undefined, response: new Response() };
}

function failedResponse(status: number): AppointmentResponse {
  return { data: undefined, error: undefined, response: new Response(null, { status }) };
}

function withVersion(version: number): AppointmentDto {
  return { ...APPOINTMENT, audit: { ...APPOINTMENT.audit, version } };
}

function createStore(lunaAppointment = fakeLunaAppointmentClient()) {
  return { store: new AppointmentStore(lunaAppointment.client), lunaAppointment };
}

async function loadedStore(lunaAppointment = fakeLunaAppointmentClient()) {
  const created = createStore(lunaAppointment);
  await created.store.load('appointment-1');
  return created;
}

describe('AppointmentStore', () => {
  it('that loading a appointment should result into the loaded appointment', async () => {
    // given
    const { store, lunaAppointment } = createStore();

    // when
    const loading = store.load('appointment-1');
    const stateWhileLoading = store.loadState;
    await loading;

    // then
    expect(stateWhileLoading).toBe('loading');
    expect(lunaAppointment.GET).toHaveBeenCalledWith('/appointments/{id}', {
      params: { path: { id: 'appointment-1' } },
    });
    expect(store.id).toBe('appointment-1');
    expect(store.appointment).toEqual(APPOINTMENT);
    expect(store.loadState).toBe('loaded');
  });

  it('that loading an unknown appointment should result into not found', async () => {
    // given
    const { store } = createStore(fakeLunaAppointmentClient(undefined, null));

    // when
    await store.load('unknown');

    // then
    expect(store.appointment).toBeNull();
    expect(store.loadState).toBe('notFound');
  });

  it('that a server error while loading should result into a failed load', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockResolvedValueOnce(failedResponse(500));

    // when
    await store.load('appointment-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that an unreachable backend while loading should result into a failed load', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    lunaAppointment.GET.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.load('appointment-1');

    // then
    expect(store.loadState).toBe('failed');
  });

  it('that loading another appointment should result into no appointment of the previous one meanwhile', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    let resolveSecond: (response: AppointmentResponse) => void = () => undefined;
    lunaAppointment.GET.mockReturnValueOnce(new Promise((resolve) => (resolveSecond = resolve)));

    // when
    const loading = store.load('appointment-2');
    const appointmentWhileLoading = store.appointment;
    resolveSecond(appointmentResponse(withVersion(7)));
    await loading;

    // then
    expect(appointmentWhileLoading).toBeNull();
    expect(store.appointment?.audit.version).toBe(7);
  });

  it('that a late answer for a previous appointment should result into the latest appointment only', async () => {
    // given
    const { store, lunaAppointment } = createStore();
    let resolveFirst: (response: AppointmentResponse) => void = () => undefined;
    let rejectOther: (error: Error) => void = () => undefined;
    lunaAppointment.GET.mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockReturnValueOnce(new Promise((_, reject) => (rejectOther = reject)))
      .mockResolvedValueOnce(appointmentResponse(withVersion(9)));
    const first = store.load('appointment-1');
    const other = store.load('appointment-2');
    await store.load('appointment-3');

    // when
    resolveFirst(appointmentResponse(withVersion(1)));
    rejectOther(new Error('network down'));
    await Promise.all([first, other]);

    // then
    expect(store.id).toBe('appointment-3');
    expect(store.appointment?.audit.version).toBe(9);
    expect(store.loadState).toBe('loaded');
  });

  it('that reloading should result into loading the current appointment again', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();

    // when
    await store.reload();

    // then
    expect(lunaAppointment.GET).toHaveBeenCalledTimes(2);
  });

  it('that reloading without appointment should result into no request', async () => {
    // given
    const { store, lunaAppointment } = createStore();

    // when
    await store.reload();

    // then
    expect(lunaAppointment.GET).not.toHaveBeenCalled();
  });

  it('that saving changes should result into an update with the loaded version', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    lunaAppointment.PUT.mockResolvedValueOnce(appointmentResponse(withVersion(4)));

    // when
    const saving = store.save(CHANGES);
    const savingWhileRunning = store.saving;
    const saved = await saving;

    // then
    expect(savingWhileRunning).toBe(true);
    expect(saved).toBe(true);
    expect(lunaAppointment.PUT).toHaveBeenCalledWith('/appointments/{id}', {
      params: { path: { id: 'appointment-1' } },
      body: { ...CHANGES, version: 1 },
    });
    expect(store.appointment?.audit.version).toBe(4);
    expect(store.saving).toBe(false);
    expect(store.saveError).toBeNull();
  });

  it.each([
    [409, 'conflict'],
    [400, 'invalid'],
    [500, 'failed'],
  ] as const)('that a status %i while saving should result into the save error %s', async (status, error) => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    lunaAppointment.PUT.mockResolvedValueOnce(failedResponse(status));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe(error);
    expect(store.appointment).toEqual(APPOINTMENT);
  });

  it('that an unreachable backend while saving should result into a failed save', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    lunaAppointment.PUT.mockRejectedValueOnce(new Error('network down'));

    // when
    const saved = await store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(store.saveError).toBe('failed');
  });

  it('that saving without loaded appointment should result into no request', async () => {
    // given
    const { store, lunaAppointment } = createStore(fakeLunaAppointmentClient(undefined, null));
    await store.load('unknown');

    // when
    const saved = await store.save(CHANGES);
    const savedWithoutId = await createStore(lunaAppointment).store.save(CHANGES);

    // then
    expect(saved).toBe(false);
    expect(savedWithoutId).toBe(false);
    expect(lunaAppointment.PUT).not.toHaveBeenCalled();
  });

  it('that cancelling should result into the cancelled appointment', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    const cancelled: AppointmentDto = { ...withVersion(4), data: { ...APPOINTMENT.data, status: 'CANCELLED' } };
    lunaAppointment.POST.mockResolvedValueOnce(appointmentResponse(cancelled));

    // when
    const cancelling = store.cancel();
    const changingWhileRunning = store.statusChanging;
    await cancelling;

    // then
    expect(changingWhileRunning).toBe(true);
    expect(lunaAppointment.POST).toHaveBeenCalledWith('/appointments/{id}/cancel', {
      params: { path: { id: 'appointment-1' } },
    });
    expect(store.appointment?.data.status).toBe('CANCELLED');
    expect(store.statusChanging).toBe(false);
  });

  it('that restoring should result into a request to restore', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();

    // when
    await store.restore();

    // then
    expect(lunaAppointment.POST).toHaveBeenCalledWith('/appointments/{id}/restore', {
      params: { path: { id: 'appointment-1' } },
    });
    expect(store.statusError).toBe(false);
  });

  it('that a rejected status change should result into a status error', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    lunaAppointment.POST.mockResolvedValueOnce(failedResponse(404));

    // when
    await store.cancel();

    // then
    expect(store.statusError).toBe(true);
    expect(store.appointment).toEqual(APPOINTMENT);
  });

  it('that an unreachable backend while changing the status should result into a status error', async () => {
    // given
    const { store, lunaAppointment } = await loadedStore();
    lunaAppointment.POST.mockRejectedValueOnce(new Error('network down'));

    // when
    await store.restore();

    // then
    expect(store.statusError).toBe(true);
    expect(store.statusChanging).toBe(false);
  });

  it('that changing the status without appointment should result into no request', async () => {
    // given
    const { store, lunaAppointment } = createStore();

    // when
    await store.cancel();

    // then
    expect(lunaAppointment.POST).not.toHaveBeenCalled();
  });
});
