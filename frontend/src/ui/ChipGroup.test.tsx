import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ChipGroup } from './ChipGroup';

const OPTIONS = [
  { value: 'auto', label: 'Automatic', icon: <span /> },
  { value: 'task', label: 'Task', icon: <span /> },
] as const;

describe('ChipGroup', () => {
  it('that choosing a chip should result into a change to its value', async () => {
    // given
    const onChange = vi.fn();
    render(<ChipGroup label="Type" options={OPTIONS} value="auto" onChange={onChange} />);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Task' }));

    // then
    expect(screen.getByRole('button', { name: 'Automatic' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Task' })).toHaveAttribute('aria-pressed', 'false');
    expect(onChange).toHaveBeenCalledWith('task');
  });
});
