import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';

test('that choosing an area should result into its placeholder page', async ({ page, isMobile }) => {
  // given
  await signIn(page);
  await openNavigation(page, isMobile);

  // when
  await page
    .getByRole('navigation', { name: 'Hauptnavigation' })
    .getByRole('link', { name: /Termine/ })
    .click();

  // then
  await expect(page).toHaveURL('/calendar');
  await expect(page.getByRole('heading', { name: 'Termine' })).toBeVisible();
  await expect(page.getByText('Dieser Bereich kommt in einem späteren Schritt.')).toBeVisible();
});

test('that the navigation should result into a collapsible rail on desktop and an overlay on phones', async ({
  page,
  isMobile,
}) => {
  // given
  await signIn(page);

  // when
  if (isMobile) {
    await openNavigation(page, isMobile);
  } else {
    await page.getByRole('button', { name: 'Einklappen' }).click();
  }

  // then
  if (isMobile) {
    await expect(page.getByRole('link', { name: 'Schnelleingabe' })).toBeVisible();
    await page.getByRole('button', { name: 'Navigation schließen' }).click();
    await expect(page.getByRole('link', { name: 'Schnelleingabe' })).not.toBeInViewport();
  } else {
    await expect(page.getByRole('button', { name: 'Ausklappen' })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('button', { name: 'Ausklappen' })).toBeVisible();
  }
});

test('that switching to english should result into english texts', async ({ page }) => {
  // given
  await signIn(page);

  // when
  await page.getByRole('button', { name: 'Englisch' }).click();

  // then
  await expect(page.getByRole('heading', { name: "What's on your mind?" })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Input' })).toBeVisible();
});
