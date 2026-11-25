import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { describe, expect, it, vi } from 'vitest';

import { AppShell } from './AppShell';

function renderShell(collapsed = false) {
  const onCollapsedChange = vi.fn();
  const result = render(
    <MemoryRouter>
      <AppShell
        brand={<span>Luna</span>}
        headerEnd={<span>end</span>}
        navigationLabel="Main navigation"
        groups={[{ items: [{ to: '/', label: 'Home', icon: <span /> }] }]}
        collapsed={collapsed}
        onCollapsedChange={onCollapsedChange}
        labels={{ open: 'Open', close: 'Close', collapse: 'Collapse', expand: 'Expand' }}
        icons={{ open: <span />, collapse: <span />, expand: <span /> }}
      >
        <p>content</p>
      </AppShell>
    </MemoryRouter>,
  );
  const shell = () => result.container.querySelector('.app-shell');
  return { onCollapsedChange, shell };
}

describe('AppShell', () => {
  it('that opening the mobile navigation should result into an open navigation with a backdrop', async () => {
    // given
    const { shell } = renderShell();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    // then
    expect(shell()).toHaveClass('is-mobile-open');
    expect(screen.getByRole('button', { name: 'Open' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
  });

  it('that clicking the backdrop should result into a closed mobile navigation', async () => {
    // given
    const { shell } = renderShell();
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Close' }));

    // then
    expect(shell()).not.toHaveClass('is-mobile-open');
  });

  it('that pressing escape should result into a closed mobile navigation', async () => {
    // given
    const { shell } = renderShell();
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    // when
    await userEvent.keyboard('{Shift}');
    await userEvent.keyboard('{Escape}');

    // then
    expect(shell()).not.toHaveClass('is-mobile-open');
  });

  it('that choosing an item should result into a closed mobile navigation', async () => {
    // given
    const { shell } = renderShell();
    await userEvent.click(screen.getByRole('button', { name: 'Open' }));

    // when
    await userEvent.click(screen.getByRole('link', { name: 'Home' }));

    // then
    expect(shell()).not.toHaveClass('is-mobile-open');
  });

  it('that toggling a collapsed navigation should result into a request to expand it', async () => {
    // given
    const { onCollapsedChange } = renderShell(true);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Expand' }));

    // then
    expect(onCollapsedChange).toHaveBeenCalledWith(false);
  });
});
