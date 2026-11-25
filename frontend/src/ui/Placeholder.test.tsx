import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { Logo } from './Logo';
import { Placeholder } from './Placeholder';

describe('Placeholder', () => {
  it('that rendering a placeholder should result into its title and text', () => {
    // given
    const title = 'Tasks';

    // when
    render(<Placeholder icon={<Logo size={40} />} title={title} text="Comes later." />);

    // then
    expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
    expect(screen.getByText('Comes later.')).toBeInTheDocument();
  });
});
