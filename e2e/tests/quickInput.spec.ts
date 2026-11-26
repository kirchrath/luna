import { expect, test } from '@playwright/test';

import { signIn } from './signIn.ts';

test('that submitting an input should result into an analysed entry with the created element', async ({
  page,
}, testInfo) => {
  // given
  await signIn(page);
  const text = `Pay the electricity bill (${testInfo.project.name} ${String(Date.now())})`;
  await page.getByRole('textbox', { name: 'Eingabe' }).fill(text);

  // when
  await page.getByRole('button', { name: 'Absenden' }).click();

  // then
  await expect(page.getByRole('textbox', { name: 'Eingabe' })).toHaveValue('');
  const entry = page.getByRole('listitem').filter({ hasText: text });
  await expect(entry).toBeVisible();
  // The status changes through the push signal of luna-core; the list is never polled.
  await expect(entry.getByText('Analysiert', { exact: true })).toBeVisible();
  await expect(entry.getByText('Aufgabe', { exact: true })).toBeVisible();
});
