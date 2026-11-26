import type { AppointmentListItemDto } from '../api';

/** Times of appointments: stored as instants, shown in local time. All-day appointments start at 00:00 local time. */

const DAYS_PER_WEEK = 7;
const MONDAY_FIRST_OFFSET = 6;
const PAD = 2;

export type AppointmentTimes = Pick<AppointmentListItemDto, 'start' | 'end' | 'allDay'>;

/** One appointment on one of its days. */
export interface AppointmentDay<T extends AppointmentTimes> {
  appointment: T;
  day: Date;
  /** 1-based day of a multi-day appointment and its number of days; 1 of 1 for one-day appointments. */
  dayNumber: number;
  dayCount: number;
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function addDays(day: Date, days: number): Date {
  return new Date(day.getFullYear(), day.getMonth(), day.getDate() + days);
}

export function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function pad(value: number): string {
  return String(value).padStart(PAD, '0');
}

/** The local days an appointment covers: from its start to its end (or start) day. */
export function daysOf(appointment: AppointmentTimes): Date[] {
  const first = startOfDay(new Date(appointment.start));
  const last = startOfDay(new Date(appointment.end ?? appointment.start));
  const days: Date[] = [];
  for (let day = first; day <= last; day = addDays(day, 1)) {
    days.push(day);
  }
  return days;
}

/** Appointments split into their days, ordered by day; days before {@code from} are left out. */
export function groupByDay<T extends AppointmentTimes>(
  appointments: readonly T[],
  from?: Date,
): { day: Date; entries: AppointmentDay<T>[] }[] {
  const groups = new Map<number, { day: Date; entries: AppointmentDay<T>[] }>();
  for (const appointment of appointments) {
    const days = daysOf(appointment);
    days.forEach((day, index) => {
      if (from !== undefined && day < startOfDay(from)) {
        return;
      }
      const group = groups.get(day.getTime()) ?? { day, entries: [] };
      group.entries.push({ appointment, day, dayNumber: index + 1, dayCount: days.length });
      groups.set(day.getTime(), group);
    });
  }
  return [...groups.values()].sort((a, b) => a.day.getTime() - b.day.getTime());
}

/** The days of the month as weeks starting on Monday; days of other months are null. */
export function monthWeeks(month: Date): (Date | null)[][] {
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const leading = (first.getDay() + MONDAY_FIRST_OFFSET) % DAYS_PER_WEEK;
  const cells: (Date | null)[] = [
    ...Array.from({ length: leading }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => addDays(first, index)),
  ];
  while (cells.length % DAYS_PER_WEEK !== 0) {
    cells.push(null);
  }
  return Array.from({ length: cells.length / DAYS_PER_WEEK }, (_, week) =>
    cells.slice(week * DAYS_PER_WEEK, (week + 1) * DAYS_PER_WEEK),
  );
}

export function formatTime(dateTime: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' }).format(new Date(dateTime));
}

export function formatDay(day: Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, { weekday: 'short', day: 'numeric', month: 'long' }).format(day);
}

export function formatMonth(month: Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(month);
}

export function weekdayNames(locale: string): string[] {
  const monday = new Date(2026, 10, 23);
  const format = new Intl.DateTimeFormat(locale, { weekday: 'short' });
  return Array.from({ length: DAYS_PER_WEEK }, (_, index) => format.format(addDays(monday, index)));
}

/** Local date and time for the inputs of the form; all-day appointments have no time. */
export function toDateInput(dateTime: string | undefined): { date: string; time: string } {
  if (dateTime === undefined) {
    return { date: '', time: '' };
  }
  const value = new Date(dateTime);
  return {
    date: `${String(value.getFullYear())}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`,
    time: `${pad(value.getHours())}:${pad(value.getMinutes())}`,
  };
}

/** The instant of a local date and time; all-day values are stored at 00:00 local time. */
export function fromDateInput(date: string, time: string, allDay: boolean): string | undefined {
  if (date === '') {
    return undefined;
  }
  return new Date(`${date}T${allDay || time === '' ? '00:00' : time}`).toISOString();
}
