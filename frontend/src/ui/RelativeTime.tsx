const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const UNITS: readonly [Intl.RelativeTimeFormatUnit, number][] = [
  ['day', DAY],
  ['hour', HOUR],
  ['minute', MINUTE],
];

interface RelativeTimeProps {
  dateTime: string;
  locale: string;
  now?: Date;
}

/** Shows a point in time relative to now ("5 minutes ago") with the exact date and time as tooltip. */
export function RelativeTime({ dateTime, locale, now = new Date() }: RelativeTimeProps) {
  const date = new Date(dateTime);
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000);
  const format = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
  const [unit, size] = UNITS.find(([, unitSeconds]) => Math.abs(seconds) >= unitSeconds) ?? ['second', 1];
  const text = format.format(Math.round(seconds / size), unit);
  const exact = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  return (
    <time dateTime={dateTime} title={exact}>
      {text}
    </time>
  );
}
