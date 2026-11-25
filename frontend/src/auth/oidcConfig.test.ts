import { WebStorageStateStore, type User } from 'oidc-client-ts';
import { describe, expect, it } from 'vitest';

import { createSignInState, createUserManager, onSigninCallback } from './oidcConfig';

const ENV = {
  VITE_OIDC_AUTHORITY: 'http://localhost:7080/auth/realms/luna',
  VITE_OIDC_CLIENT_ID: 'luna-frontend',
} as ImportMetaEnv;

describe('oidcConfig', () => {
  it('that creating the user manager should result into the authority, client and redirects of the app', () => {
    // given
    const origin = window.location.origin;

    // when
    const { settings } = createUserManager(ENV);

    // then
    expect(settings.authority).toBe('http://localhost:7080/auth/realms/luna');
    expect(settings.client_id).toBe('luna-frontend');
    expect(settings.redirect_uri).toBe(`${origin}/`);
    expect(settings.post_logout_redirect_uri).toBe(`${origin}/`);
    expect(settings.userStore).toBeInstanceOf(WebStorageStateStore);
    expect(settings.automaticSilentRenew).toBe(true);
  });

  it('that creating the user manager without arguments should result into the vite environment', () => {
    // given
    const env = import.meta.env;

    // when
    const { settings } = createUserManager();

    // then
    expect(settings.client_id).toBe(env.VITE_OIDC_CLIENT_ID);
  });

  it('that returning from keycloak without requested page should result into an address without code and state', () => {
    // given
    window.history.replaceState({}, '', '/tasks?code=abc&state=xyz');

    // when
    onSigninCallback(undefined);

    // then
    expect(window.location.pathname).toBe('/tasks');
    expect(window.location.search).toBe('');
  });

  it('that returning from keycloak should result into the page requested before signing in', () => {
    // given
    window.history.replaceState({}, '', '/calendar');
    const state = createSignInState();
    window.history.replaceState({}, '', '/?code=abc&state=xyz');

    // when
    onSigninCallback({ state } as unknown as User);

    // then
    expect(window.location.pathname).toBe('/calendar');
    expect(window.location.search).toBe('');
  });

  it('that a return target outside the app should result into the current page', () => {
    // given
    window.history.replaceState({}, '', '/?code=abc&state=xyz');

    // when
    onSigninCallback({ state: { returnTo: '//evil.example/' } } as unknown as User);

    // then
    expect(window.location.pathname).toBe('/');
    expect(window.location.search).toBe('');
  });
});
