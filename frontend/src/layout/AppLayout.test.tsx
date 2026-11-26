import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { readPreference, STORAGE_KEYS, writePreference } from '../services';
import { fakeAuth, fakeUser, renderApp } from '../test';

describe('AppLayout', () => {
  it('that opening the start page should result into the quick input with all areas in the navigation', async () => {
    // given
    const path = '/';

    // when
    await renderApp(path);

    // then
    expect(screen.getByRole('heading', { name: 'Was steht an?' })).toBeInTheDocument();
    const navigation = screen.getByRole('navigation', { name: 'Hauptnavigation' });
    expect(within(navigation).getByRole('link', { name: 'Schnelleingabe' })).toHaveClass('active');
    for (const area of ['Aufgaben', 'Termine', 'Einkauf & Haushalt', 'Finanzen & Verträge', 'Monitoring']) {
      expect(within(navigation).getByRole('link', { name: area })).toBeInTheDocument();
    }
    expect(within(navigation).getAllByText('bald')).toHaveLength(4);
    expect(within(within(navigation).getByRole('link', { name: 'Aufgaben' })).queryByText('bald')).toBeNull();
  });

  it('that choosing an area should result into its placeholder page', async () => {
    // given
    const { router } = await renderApp('/');
    const navigation = screen.getByRole('navigation', { name: 'Hauptnavigation' });

    // when
    await userEvent.click(within(navigation).getByRole('link', { name: /Termine/ }));

    // then
    expect(router.state.location.pathname).toBe('/calendar');
    expect(screen.getByRole('heading', { name: 'Termine' })).toBeInTheDocument();
    expect(screen.getByText('Dieser Bereich kommt in einem späteren Schritt.')).toBeInTheDocument();
  });

  it('that collapsing the navigation should result into a remembered collapsed navigation', async () => {
    // given
    const { container } = await renderApp('/');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Einklappen' }));

    // then
    expect(container.querySelector('.app-shell')).toHaveClass('is-collapsed');
    expect(screen.getByRole('button', { name: 'Ausklappen' })).toHaveAttribute('aria-expanded', 'false');
    expect(readPreference(STORAGE_KEYS.navigationCollapsed)).toBe('true');
  });

  it('that a remembered collapsed navigation should result into a collapsed navigation on start', async () => {
    // given
    writePreference(STORAGE_KEYS.navigationCollapsed, 'true');

    // when
    const { container } = await renderApp('/');

    // then
    expect(container.querySelector('.app-shell')).toHaveClass('is-collapsed');
  });

  it('that switching to English should result into English texts', async () => {
    // given
    await renderApp('/');

    // when
    await userEvent.click(screen.getByRole('button', { name: 'Englisch' }));

    // then
    expect(await screen.findByRole('heading', { name: "What's on your mind?" })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'German' })).toHaveAttribute('aria-pressed', 'false');
    expect(readPreference(STORAGE_KEYS.language)).toBe('en');
  });

  it('that a signed in user should result into the first name in the user menu', async () => {
    // given
    const path = '/';

    // when
    await renderApp(path);

    // then
    expect(screen.getByRole('button', { name: 'Benutzermenü' })).toHaveTextContent('Chandra');
  });

  it('that a user without first name should result into the user name in the user menu', async () => {
    // given
    const auth = fakeAuth({ user: fakeUser({ preferred_username: 'chandra' }) });

    // when
    await renderApp('/', { auth });

    // then
    expect(screen.getByRole('button', { name: 'Benutzermenü' })).toHaveTextContent('chandra');
  });

  it('that a missing user should result into an empty name in the user menu', async () => {
    // given
    const auth = fakeAuth({ user: null });

    // when
    await renderApp('/', { auth });

    // then
    expect(screen.getByRole('button', { name: 'Benutzermenü' })).toHaveTextContent('');
  });

  it('that signing out should result into a sign out at keycloak', async () => {
    // given
    const signoutRedirect = vi.fn(() => Promise.resolve());
    await renderApp('/', { auth: fakeAuth({ signoutRedirect }) });
    await userEvent.click(screen.getByRole('button', { name: 'Benutzermenü' }));

    // when
    await userEvent.click(screen.getByRole('menuitem', { name: 'Abmelden' }));

    // then
    expect(signoutRedirect).toHaveBeenCalledTimes(1);
  });
});
