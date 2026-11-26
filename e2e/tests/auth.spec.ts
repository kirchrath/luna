import { expect, test } from '@playwright/test';

import { TEST_USER } from '../environment/settings.ts';
import { KEYCLOAK_LOGIN, signIn } from './signIn.ts';

test('that opening a page without session should result into the keycloak login and then the requested page', async ({
  page,
}) => {
  // given
  const path = '/calendar';

  // when
  await signIn(page, path);

  // then
  await expect(page).toHaveURL(path);
  await expect(page.getByRole('heading', { name: 'Termine' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Benutzermenü' })).toContainText(TEST_USER.firstName);
});

test('that signing out should result into the keycloak login', async ({ page }) => {
  // given
  await signIn(page);
  await page.getByRole('button', { name: 'Benutzermenü' }).click();

  // when
  await page.getByRole('menuitem', { name: 'Abmelden' }).click();

  // then
  await page.waitForURL(KEYCLOAK_LOGIN);
  await expect(page.locator('#username')).toBeVisible();
});
