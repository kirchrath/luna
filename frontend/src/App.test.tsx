import { render, screen } from '@testing-library/react';
import { AuthContext } from 'react-oidc-context';
import { describe, expect, it } from 'vitest';

import { App } from './App';
import { initI18n } from './i18n';
import { fakeAuth, fakeRootStore } from './test';

describe('App', () => {
  it('that rendering the app should result into the page of the current address', async () => {
    // given
    await initI18n();
    window.history.replaceState({}, '', '/calendar');

    // when
    render(
      <AuthContext value={fakeAuth()}>
        <App rootStore={fakeRootStore().rootStore} />
      </AuthContext>,
    );

    // then
    expect(await screen.findByRole('heading', { name: 'Termine' })).toBeInTheDocument();
  });
});
