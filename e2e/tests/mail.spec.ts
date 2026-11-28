import { expect, test } from '@playwright/test';

import { signIn } from './signIn.ts';
import { sendMail } from './sendMail.ts';

test('that a new email should result into an analyzed input with the email in the recent inputs', async ({
  page,
}, testInfo) => {
  // given
  const marker = `[e2e-mail-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  const list = page.getByRole('region', { name: 'Letzte Eingaben' });
  await expect(list).toBeVisible();

  // when
  await sendMail(`Invoice ${marker}`, 'Please pay 42 EUR by Friday.');

  // then
  // luna-mail fetches the email, luna-core shows it through its push signal; the list is never polled.
  const input = list.getByRole('listitem').filter({ hasText: marker });
  await expect(input).toBeVisible({ timeout: 30_000 });
  await expect(input.getByText('Power company <billing@power.test>')).toBeVisible();
  await expect(input.getByRole('img', { name: 'E-Mail' })).toBeVisible();
  // The test account belongs to the work area.
  await expect(input.getByRole('img', { name: 'Beruflich' })).toBeVisible();
  await expect(input.getByText('Analysiert')).toBeVisible();
  await expect(input.getByText('Aufgabe')).toBeVisible();
});
