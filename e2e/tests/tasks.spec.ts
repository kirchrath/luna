import { expect, test, type Page } from '@playwright/test';

import { BASE_URL, CLIENT_ID, REALM } from '../environment/settings.ts';
import { openNavigation, signIn } from './signIn.ts';

const OIDC_USER_KEY = `oidc.user:${BASE_URL}/auth/realms/${REALM}:${CLIENT_ID}`;

/** Submits a manual input to luna-core with the token of the signed in user, without leaving the page. */
async function submitInput(page: Page, text: string): Promise<void> {
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
    { key: OIDC_USER_KEY, body: { text } },
  );
  expect(status).toBe(202);
}

test('that an input should result into a task that can be edited and completed', async ({
  page,
  isMobile,
}, testInfo) => {
  // given
  const marker = `[e2e-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  await openNavigation(page, isMobile);
  await page.getByRole('link', { name: 'Aufgaben' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Aufgaben' })).toBeVisible();

  // when
  await submitInput(page, `Pay the electricity bill ${marker}`);

  // then
  // The task appears through the push signal of luna-task; the list is never polled.
  const task = page.getByRole('listitem').filter({ hasText: marker });
  await expect(task).toBeVisible();
  await expect(task.getByText('ohne Fälligkeit')).toBeVisible();

  // when
  await task.getByRole('link').click();
  await page.getByLabel('Titel').fill(`Edited task ${marker}`);
  await page.getByRole('button', { name: 'Hoch' }).click();
  await page.getByRole('button', { name: 'Speichern' }).click();

  // then
  await expect(page.getByRole('heading', { level: 1, name: `Edited task ${marker}` })).toBeVisible();
  await expect(
    page.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
  ).toBeVisible();

  // when
  await page.getByRole('button', { name: 'Erledigen' }).click();

  // then
  await expect(page.getByRole('button', { name: 'Wieder öffnen' })).toBeVisible();

  // when
  await page.getByRole('main').getByRole('link', { name: 'Aufgaben' }).click();
  await page.getByRole('button', { name: 'Erledigt' }).click();

  // then
  const done = page.getByRole('listitem').filter({ hasText: marker });
  await expect(done).toBeVisible();
  await expect(done.getByRole('checkbox')).toHaveAttribute('aria-checked', 'true');
  await expect(done.getByText('Hoch')).toBeVisible();
});
