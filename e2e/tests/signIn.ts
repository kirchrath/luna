import { expect, type Page } from '@playwright/test';

import { TEST_USER } from '../environment/settings.ts';

export const KEYCLOAK_LOGIN = /\/auth\/realms\/luna\/protocol\/openid-connect\/auth/;

/** Opens a page of Luna, signs in at Keycloak and waits until Luna shows the requested page again. */
export async function signIn(page: Page, path = '/'): Promise<void> {
  await page.goto(path);
  await page.waitForURL(KEYCLOAK_LOGIN);
  await page.locator('#username').fill(TEST_USER.username);
  await page.locator('#password').fill(TEST_USER.password);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('button', { name: 'Benutzermenü' })).toBeVisible();
}

/** On small screens the navigation is an overlay that has to be opened first. */
export async function openNavigation(page: Page, isMobile: boolean): Promise<void> {
  if (isMobile) {
    await page.getByRole('button', { name: 'Navigation öffnen' }).click();
  }
}
