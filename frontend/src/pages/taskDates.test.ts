import { describe, expect, it } from 'vitest';

import { formatDateTime, formatDueDate, fromDueDateInput, hasTime, isOverdue, toDueDateInput } from './taskDates';

/** Local time, independent of the time zone the tests run in. */
function local(year: number, month: number, day: number, hours = 0, minutes = 0): Date {
  return new Date(year, month - 1, day, hours, minutes);
}

const NOW = local(2026, 11, 26, 14, 30);

describe('taskDates', () => {
  it('that a due date at local midnight should result into a due date without time', () => {
    // when / then
    expect(hasTime(local(2026, 12, 5))).toBe(false);
    expect(hasTime(local(2026, 12, 5, 0, 30))).toBe(true);
    expect(hasTime(local(2026, 12, 5, 10))).toBe(true);
  });

  it('that a due date with time should result into date and time fields', () => {
    // given
    const dueDate = local(2026, 12, 1, 9, 5).toISOString();

    // when
    const input = toDueDateInput(dueDate);

    // then
    expect(input).toEqual({ date: '2026-12-01', time: '09:05' });
  });

  it('that a due date without time or no due date should result into empty fields', () => {
    // when / then
    expect(toDueDateInput(local(2026, 12, 5).toISOString())).toEqual({ date: '2026-12-05', time: '' });
    expect(toDueDateInput(undefined)).toEqual({ date: '', time: '' });
  });

  it('that date and time fields should result into the instant of the local time', () => {
    // when / then
    expect(fromDueDateInput({ date: '2026-12-01', time: '09:05' })).toBe(local(2026, 12, 1, 9, 5).toISOString());
    expect(fromDueDateInput({ date: '2026-12-05', time: '' })).toBe(local(2026, 12, 5).toISOString());
    expect(fromDueDateInput({ date: '', time: '09:05' })).toBeUndefined();
  });

  it('that a due date with time before now should result into an overdue task', () => {
    // when / then
    expect(isOverdue(local(2026, 11, 26, 14).toISOString(), NOW)).toBe(true);
    expect(isOverdue(local(2026, 11, 26, 15).toISOString(), NOW)).toBe(false);
  });

  it('that a due date without time should result into an overdue task only from the next day on', () => {
    // when / then
    expect(isOverdue(local(2026, 11, 26).toISOString(), NOW)).toBe(false);
    expect(isOverdue(local(2026, 11, 25).toISOString(), NOW)).toBe(true);
  });

  it('that a due date should result into day and month, with time and year only where needed', () => {
    // when / then
    expect(formatDueDate(local(2026, 12, 5).toISOString(), 'de', NOW)).toBe('05.12.');
    expect(formatDueDate(local(2026, 12, 1, 10).toISOString(), 'de', NOW)).toBe('01.12., 10:00');
    expect(formatDueDate(local(2027, 1, 7).toISOString(), 'de', NOW)).toBe('07.01.2027');
  });

  it('that a point in time should result into date and time', () => {
    // when
    const formatted = formatDateTime(local(2026, 11, 24, 9, 12).toISOString(), 'de');

    // then
    expect(formatted).toBe('24.11.2026, 09:12');
  });
});
