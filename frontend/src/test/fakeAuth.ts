import type { User } from 'oidc-client-ts';
import type { AuthContextProps } from 'react-oidc-context';
import { vi } from 'vitest';

export function fakeUser(profile: Record<string, string> = { given_name: 'Chandra' }): User {
  return { profile: { sub: 'user-id', ...profile } } as unknown as User;
}

export function fakeAuth(overrides: Partial<AuthContextProps> = {}): AuthContextProps {
  return {
    isLoading: false,
    isAuthenticated: true,
    user: fakeUser(),
    activeNavigator: undefined,
    error: undefined,
    signinRedirect: vi.fn(() => Promise.resolve()),
    signoutRedirect: vi.fn(() => Promise.resolve()),
    ...overrides,
  } as AuthContextProps;
}
