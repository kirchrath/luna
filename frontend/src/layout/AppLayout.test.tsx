import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { readPreference, STORAGE_KEYS, writePreference } from '../services/storage';
import { renderApp } from '../test/renderApp';

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
    expect(within(navigation).getAllByText('bald')).toHaveLength(5);
  });

  it('that choosing an area should result into its placeholder page', async () => {
    // given
    const { router } = await renderApp('/');
    const navigation = screen.getByRole('navigation', { name: 'Hauptnavigation' });

    // when
    await userEvent.click(within(navigation).getByRole('link', { name: 'Aufgaben' }));

    // then
    expect(router.state.location.pathname).toBe('/tasks');
    expect(screen.getByRole('heading', { name: 'Aufgaben' })).toBeInTheDocument();
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
});
