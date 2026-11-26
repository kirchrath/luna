import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';
import { submitInput } from './submitInput.ts';

test('that an alert input should result into an alert that can be acknowledged and resolved', async ({
  page,
  isMobile,
}, testInfo) => {
  // given
  const marker = `[e2e-alert-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  await openNavigation(page, isMobile);
  await page.getByRole('link', { name: 'Warnungen' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Warnungen' })).toBeVisible();

  // when
  await submitInput(page, `The NAS disk is almost full ${marker}`);

  // then
  // The alert appears through the push signal of luna-alert; the list is never polled.
  const alert = page.getByRole('listitem').filter({ hasText: marker });
  await expect(alert).toBeVisible();
  await expect(alert.getByText('Kritisch')).toBeVisible();

  // when
  await alert.getByRole('button', { name: /bestätigen$/ }).click();

  // then
  await expect(alert.getByText('Bestätigt', { exact: true })).toBeVisible();

  // when
  await alert.getByRole('link').click();
  await page.getByRole('button', { name: 'Als behoben' }).click();

  // then
  await expect(page.getByRole('button', { name: 'Wieder öffnen' })).toBeVisible();
  await expect(page.getByText('Behoben', { exact: true })).toBeVisible();

  // when
  await page.getByRole('main').getByRole('link', { name: 'Warnungen' }).click();

  // then
  await expect(page.getByRole('heading', { level: 1, name: 'Warnungen' })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: marker })).toHaveCount(0);
});
