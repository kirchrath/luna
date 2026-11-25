import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UserMenu } from './UserMenu';

function renderMenu() {
  const onLogout = vi.fn();
  render(
    <div>
      <p>outside</p>
      <UserMenu
        label="User menu"
        name="Chandra"
        icon={<span />}
        logoutLabel="Sign out"
        logoutIcon={<span />}
        onLogout={onLogout}
      />
    </div>,
  );
  return { onLogout };
}

describe('UserMenu', () => {
  it('that opening the menu should result into the sign out entry', async () => {
    // given
    renderMenu();

    // when
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // then
    expect(screen.getByRole('button', { name: 'User menu' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('menuitem', { name: 'Sign out' })).toBeInTheDocument();
    expect(screen.getByText('Chandra')).toBeInTheDocument();
  });

  it('that choosing sign out should result into a sign out request', async () => {
    // given
    const { onLogout } = renderMenu();
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // when
    await userEvent.click(screen.getByRole('menuitem', { name: 'Sign out' }));

    // then
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it('that clicking outside should result into a closed menu', async () => {
    // given
    renderMenu();
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // when
    await userEvent.click(screen.getByText('outside'));

    // then
    expect(screen.queryByRole('menuitem')).not.toBeInTheDocument();
  });

  it('that pressing escape should result into a closed menu', async () => {
    // given
    renderMenu();
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // when
    await userEvent.keyboard('{Shift}');
    await userEvent.keyboard('{Escape}');

    // then
    expect(screen.queryByRole('menuitem')).not.toBeInTheDocument();
  });

  it('that clicking the button of an open menu should result into a closed menu', async () => {
    // given
    renderMenu();
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // when
    await userEvent.click(screen.getByRole('button', { name: 'User menu' }));

    // then
    expect(screen.queryByRole('menuitem')).not.toBeInTheDocument();
  });
});
