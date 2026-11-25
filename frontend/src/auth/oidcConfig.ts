import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

const PATH_SEPARATOR = '/';

interface SignInState {
  returnTo: string;
}

/** State sent to Keycloak so that the app returns to the requested page after signing in. */
export function createSignInState(): SignInState {
  return { returnTo: window.location.pathname };
}

function returnPath(user: User | undefined): string {
  const returnTo = (user?.state as Partial<SignInState> | undefined)?.returnTo;
  // Only paths within the app are accepted, never other origins.
  return typeof returnTo === 'string' && returnTo.startsWith(PATH_SEPARATOR) && !returnTo.startsWith('//')
    ? returnTo
    : window.location.pathname;
}

/** Replaces code and state of the authorization response with the page requested before signing in. */
export function onSigninCallback(user: User | undefined): void {
  window.history.replaceState({}, document.title, returnPath(user));
}

/** The single UserManager of the app, shared by the AuthProvider and the API clients. */
export function createUserManager(env: ImportMetaEnv = import.meta.env): UserManager {
  const appUrl = `${window.location.origin}${PATH_SEPARATOR}`;
  return new UserManager({
    authority: env.VITE_OIDC_AUTHORITY,
    client_id: env.VITE_OIDC_CLIENT_ID,
    redirect_uri: appUrl,
    post_logout_redirect_uri: appUrl,
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: true,
  });
}
