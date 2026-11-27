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

  it('that options with icons should result into buttons named by their labels and an extra class', () => {
    // given
    const options = [
      { value: 'all', label: 'All', icon: <svg data-testid="all-icon" /> },
      { value: 'work', label: 'Work', icon: <svg data-testid="work-icon" /> },
    ] as const;

    // when
    render(<SegmentedControl label="Area" options={options} value="work" onChange={vi.fn()} className="is-compact" />);

    // then
    expect(screen.getByRole('group', { name: 'Area' })).toHaveClass('segmented-control', 'is-compact');
    expect(screen.getByRole('button', { name: 'Work' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'All' })).toHaveAttribute('title', 'All');
    expect(screen.getByTestId('all-icon')).toBeInTheDocument();
  });
});
