/** Due dates of tasks: stored as instants, entered as local date with optional local time. */

const DATE_SEPARATOR = 'T';
const MIDNIGHT = '00:00';
const PAD = 2;

export interface DueDateInput {
  date: string;
  time: string;
}

function pad(value: number): string {
  return String(value).padStart(PAD, '0');
}

/** A due date without its own time is stored at local midnight. */
export function hasTime(dueDate: Date): boolean {
  return dueDate.getHours() !== 0 || dueDate.getMinutes() !== 0;
}

export function toDueDateInput(dueDate: string | undefined): DueDateInput {
  if (dueDate === undefined) {
    return { date: '', time: '' };
  }
  const due = new Date(dueDate);
  return {
    date: `${String(due.getFullYear())}-${pad(due.getMonth() + 1)}-${pad(due.getDate())}`,
    time: hasTime(due) ? `${pad(due.getHours())}:${pad(due.getMinutes())}` : '',
  };
}

export function fromDueDateInput({ date, time }: DueDateInput): string | undefined {
  if (date === '') {
    return undefined;
  }
  return new Date(`${date}${DATE_SEPARATOR}${time === '' ? MIDNIGHT : time}`).toISOString();
}

/** Overdue are tasks due before now, or before today if they have no time of their own. */
export function isOverdue(dueDate: string, now: Date): boolean {
  const due = new Date(dueDate);
  if (hasTime(due)) {
    return due < now;
  }
  return due < new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

/** E.g. "01.12." or "01.12., 10:00"; the year is shown when it is not the current one. */
export function formatDueDate(dueDate: string, locale: string, now: Date): string {
  const due = new Date(dueDate);
  const sameYear = due.getFullYear() === now.getFullYear();
  return new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: '2-digit',
    ...(sameYear ? {} : { year: 'numeric' }),
    ...(hasTime(due) ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(due);
}

export function formatDateTime(dateTime: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(dateTime));
}
