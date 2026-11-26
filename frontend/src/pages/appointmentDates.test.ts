import { describe, expect, it } from 'vitest';

import {
  daysOf,
  formatDay,
  formatMonth,
  formatTime,
  fromDateInput,
  groupByDay,
  monthWeeks,
  toDateInput,
  weekdayNames,
} from './appointmentDates';

function local(year: number, month: number, day: number, hours = 0, minutes = 0): string {
  return new Date(year, month - 1, day, hours, minutes).toISOString();
}

describe('appointmentDates', () => {
  it('that a multi-day appointment should result into one entry per day', () => {
    // given
    const appointment = { start: local(2026, 11, 30, 18), end: local(2026, 12, 2, 10), allDay: false };

    // when
    const groups = groupByDay([appointment]);

    // then
    expect(groups.map(({ day }) => day)).toEqual([
      new Date(2026, 10, 30),
      new Date(2026, 11, 1),
      new Date(2026, 11, 2),
    ]);
    expect(groups.map(({ entries }) => [entries[0]?.dayNumber, entries[0]?.dayCount])).toEqual([
      [1, 3],
      [2, 3],
      [3, 3],
    ]);
  });

  it('that grouping from a day should result into no days before it and days in order', () => {
    // given
    const later = { start: local(2026, 12, 3, 9), allDay: false };
    const running = { start: local(2026, 11, 30, 9), end: local(2026, 12, 2, 0), allDay: true };
    const sameDay = { start: local(2026, 12, 2, 8), allDay: false };

    // when
    const groups = groupByDay([later, running, sameDay], new Date(2026, 11, 1, 12));

    // then
    expect(groups.map(({ day }) => day.getDate())).toEqual([1, 2, 3]);
    expect(groups[1]?.entries.map(({ appointment }) => appointment)).toEqual([running, sameDay]);
  });

  it('that an appointment without end should result into its start day only', () => {
    // given
    const appointment = { start: local(2026, 12, 1, 9), allDay: false };

    // when
    const days = daysOf(appointment);

    // then
    expect(days).toEqual([new Date(2026, 11, 1)]);
  });

  it('that a month should result into weeks starting on Monday', () => {
    // given
    const november = new Date(2026, 10, 1);

    // when
    const weeks = monthWeeks(november);

    // then
    expect(weeks).toHaveLength(6);
    expect(weeks[0]?.slice(0, 6)).toEqual([null, null, null, null, null, null]);
    expect(weeks[0]?.[6]).toEqual(new Date(2026, 10, 1));
    expect(weeks[5]?.[0]).toEqual(new Date(2026, 10, 30));
    expect(weeks[5]?.[1]).toBeNull();
  });

  it('that a month starting on Monday should result into no leading empty days', () => {
    // given
    const june = new Date(2026, 5, 1);

    // when
    const weeks = monthWeeks(june);

    // then
    expect(weeks[0]?.[0]).toEqual(new Date(2026, 5, 1));
  });

  it('that formatting should result into localized days, months, times and weekdays', () => {
    // given
    const day = new Date(2026, 11, 1, 9, 5);

    // when
    const formatted = [formatDay(day, 'de'), formatMonth(day, 'en'), formatTime(day.toISOString(), 'de')];
    const weekdays = weekdayNames('de');

    // then
    expect(formatted).toEqual(['Di., 1. Dezember', 'December 2026', '09:05']);
    expect(weekdays[0]).toBe('Mo');
    expect(weekdays).toHaveLength(7);
  });

  it('that converting to form inputs should result into local date and time', () => {
    // given
    const start = local(2026, 3, 4, 7, 8);

    // when
    const inputs = [toDateInput(start), toDateInput(undefined)];

    // then
    expect(inputs).toEqual([
      { date: '2026-03-04', time: '07:08' },
      { date: '', time: '' },
    ]);
  });

  it('that converting from form inputs should result into instants at local time', () => {
    // given
    const date = '2026-12-01';

    // when
    const values = [
      fromDateInput(date, '09:30', false),
      fromDateInput(date, '09:30', true),
      fromDateInput(date, '', false),
      fromDateInput('', '09:30', false),
    ];

    // then
    expect(values).toEqual([local(2026, 12, 1, 9, 30), local(2026, 12, 1), local(2026, 12, 1), undefined]);
  });
});
