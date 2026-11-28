import { expect, type Page } from '@playwright/test';

import { BASE_URL, CLIENT_ID, REALM } from '../environment/settings.ts';

const OIDC_USER_KEY = `oidc.user:${BASE_URL}/auth/realms/${REALM}:${CLIENT_ID}`;

/** Submits a manual input to luna-core with the token of the signed in user, without leaving the page. */
export async function submitInput(page: Page, text: string, area: 'PERSONAL' | 'WORK' = 'PERSONAL'): Promise<void> {
  const status = await page.evaluate(
    async ({ key, body }) => {
      const user = JSON.parse(sessionStorage.getItem(key) ?? '{}') as { access_token?: string };
      const response = await fetch('/api/luna-core/inbound-events/manual', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${user.access_token ?? ''}` },
        body: JSON.stringify(body),
      });
      return response.status;
    },
    { key: OIDC_USER_KEY, body: { text, area } },
  );
  expect(status).toBe(202);
}
