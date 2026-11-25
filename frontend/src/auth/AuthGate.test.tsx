import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import { beforeAll, describe, expect, it, vi } from 'vitest';

import { initI18n } from '../i18n';
import { fakeAuth } from '../test';
import { AuthGate } from './AuthGate';

function renderGate(auth: AuthContextProps) {
  return render(
    <AuthContext value={auth}>
      <AuthGate>
        <p>protected content</p>
      </AuthGate>
    </AuthContext>,
  );
}

describe('AuthGate', () => {
  beforeAll(async () => {
    await initI18n();
  });

  it('that an authenticated user should result into the protected content', () => {
    // given
    const signinRedirect = vi.fn(() => Promise.resolve());
    const auth = fakeAuth({ signinRedirect });

    // when
    renderGate(auth);

    // then
    expect(screen.getByText('protected content')).toBeInTheDocument();
    expect(signinRedirect).not.toHaveBeenCalled();
  });

  it('that a missing sign-in should result into a redirect to keycloak', async () => {
    // given
    const signinRedirect = vi.fn(() => Promise.resolve());
    const auth = fakeAuth({ signinRedirect, isAuthenticated: false, user: null });

    // when
    renderGate(auth);

    // then
    expect(screen.getByRole('heading', { name: 'Anmeldung läuft …' })).toBeInTheDocument();
    expect(screen.queryByText('protected content')).not.toBeInTheDocument();
    await waitFor(() => {
      expect(signinRedirect).toHaveBeenCalledWith({ state: { returnTo: window.location.pathname } });
    });
  });

  it('that a running sign-in check should result into a loading screen without redirect', () => {
    // given
    const signinRedirect = vi.fn(() => Promise.resolve());
    const auth = fakeAuth({ signinRedirect, isLoading: true, isAuthenticated: false, user: null });

    // when
    renderGate(auth);

    // then
    expect(screen.getByRole('heading', { name: 'Anmeldung läuft …' })).toBeInTheDocument();
    expect(signinRedirect).not.toHaveBeenCalled();
  });

  it('that an active navigation should result into no further redirect', () => {
    // given
    const signinRedirect = vi.fn(() => Promise.resolve());
    const auth = fakeAuth({ signinRedirect, isAuthenticated: false, user: null, activeNavigator: 'signinRedirect' });

    // when
    renderGate(auth);

    // then
    expect(signinRedirect).not.toHaveBeenCalled();
  });

  it('that a failed sign-in should result into an error with a retry', async () => {
    // given
    const signinRedirect = vi.fn(() => Promise.resolve());
    const auth = fakeAuth({
      signinRedirect,
      isAuthenticated: false,
      user: null,
      error: new Error('Keycloak unreachable') as NonNullable<AuthContextProps['error']>,
    });
    renderGate(auth);

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }));

    // then
    expect(screen.getByRole('heading', { name: 'Anmeldung fehlgeschlagen' })).toBeInTheDocument();
    expect(signinRedirect).toHaveBeenCalledTimes(1);
  });
});
