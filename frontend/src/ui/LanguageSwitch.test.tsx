import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { LanguageSwitch } from './LanguageSwitch';

const OPTIONS = [
  { value: 'de', short: 'DE', label: 'German' },
  { value: 'en', short: 'EN', label: 'English' },
] as const;

describe('LanguageSwitch', () => {
  it('that rendering the switch should result into the current language being pressed', () => {
    // given
    const onChange = vi.fn();

    // when
    render(<LanguageSwitch label="Language" options={OPTIONS} value="de" onChange={onChange} />);

    // then
    expect(screen.getByRole('button', { name: 'German' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('that choosing a language should result into a change to that language', async () => {
    // given
    const onChange = vi.fn();
    render(<LanguageSwitch label="Language" options={OPTIONS} value="de" onChange={onChange} />);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'English' }));

    // then
    expect(onChange).toHaveBeenCalledWith('en');
  });
});
