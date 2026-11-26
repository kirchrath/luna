import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { SegmentedControl } from './SegmentedControl';

const OPTIONS = [
  { value: 'open', label: 'Open' },
  { value: 'done', label: 'Done' },
] as const;

describe('SegmentedControl', () => {
  it('that choosing a segment should result into a change to its value', async () => {
    // given
    const onChange = vi.fn();
    render(<SegmentedControl label="Show" options={OPTIONS} value="open" onChange={onChange} />);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Done' }));

    // then
    expect(screen.getByRole('group', { name: 'Show' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Done' })).toHaveAttribute('aria-pressed', 'false');
    expect(onChange).toHaveBeenCalledWith('done');
  });
});
