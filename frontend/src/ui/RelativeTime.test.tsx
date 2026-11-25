import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { RelativeTime } from './RelativeTime';

const NOW = new Date('2026-11-25T12:00:00+01:00');

describe('RelativeTime', () => {
  it.each([
    ['2026-11-25T11:48:00+01:00', 'de', 'vor 12 Minuten'],
    ['2026-11-25T09:00:00+01:00', 'de', 'vor 3 Stunden'],
    ['2026-11-24T12:00:00+01:00', 'de', 'gestern'],
    ['2026-11-25T11:59:30+01:00', 'en', '30 seconds ago'],
  ])('that the point in time %s in %s should result into "%s"', (dateTime, locale, expected) => {
    // given
    const props = { dateTime, locale, now: NOW };

    // when
    render(<RelativeTime {...props} />);

    // then
    expect(screen.getByText(expected)).toHaveAttribute('datetime', dateTime);
  });

  it('that a point in time should result into the exact date and time as tooltip', () => {
    // given
    const dateTime = '2026-11-24T18:42:00+01:00';

    // when
    render(<RelativeTime dateTime={dateTime} locale="de" />);

    // then
    expect(screen.getByRole('time')).toHaveAttribute(
      'title',
      new Intl.DateTimeFormat('de', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(dateTime)),
    );
  });
});
