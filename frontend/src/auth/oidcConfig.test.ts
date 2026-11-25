import { WebStorageStateStore, type User } from 'oidc-client-ts';
import { describe, expect, it } from 'vitest';

import { createOidcConfig, createSignInState } from './oidcConfig';

const ENV = {
  VITE_OIDC_AUTHORITY: 'http://localhost:7080/auth/realms/luna',
  VITE_OIDC_CLIENT_ID: 'luna-frontend',
} as ImportMetaEnv;

describe('oidcConfig', () => {
  it('that creating the configuration should result into the authority, client and redirects of the app', () => {
    // given
    const origin = window.location.origin;

    // when
    const config = createOidcConfig(ENV);

    // then
    expect(config.authority).toBe('http://localhost:7080/auth/realms/luna');
    expect(config.client_id).toBe('luna-frontend');
    expect(config.redirect_uri).toBe(`${origin}/`);
    expect(config.post_logout_redirect_uri).toBe(`${origin}/`);
    expect(config.userStore).toBeInstanceOf(WebStorageStateStore);
    expect(config.automaticSilentRenew).toBe(true);
  });

  it('that returning from keycloak without requested page should result into an address without code and state', () => {
    // given
    window.history.replaceState({}, '', '/tasks?code=abc&state=xyz');
    const config = createOidcConfig(ENV);

    // when
    void config.onSigninCallback?.(undefined);

    // then
    expect(window.location.pathname).toBe('/tasks');
    expect(window.location.search).toBe('');
  });

  it('that creating the configuration without arguments should result into the vite environment', () => {
    // given
    const env = import.meta.env;

    // when
    const config = createOidcConfig();

    // then
    expect(config.client_id).toBe(env.VITE_OIDC_CLIENT_ID);
  });

  it('that returning from keycloak should result into the page requested before signing in', () => {
    // given
    window.history.replaceState({}, '', '/calendar');
    const state = createSignInState();
    window.history.replaceState({}, '', '/?code=abc&state=xyz');
    const config = createOidcConfig(ENV);

    // when
    void config.onSigninCallback?.({ state } as unknown as User);

    // then
    expect(window.location.pathname).toBe('/calendar');
    expect(window.location.search).toBe('');
  });

  it('that a return target outside the app should result into the current page', () => {
    // given
    window.history.replaceState({}, '', '/?code=abc&state=xyz');
    const config = createOidcConfig(ENV);

    // when
    void config.onSigninCallback?.({ state: { returnTo: '//evil.example/' } } as unknown as User);

    // then
    expect(window.location.pathname).toBe('/');
    expect(window.location.search).toBe('');
  });
});
