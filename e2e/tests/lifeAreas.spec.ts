import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';
import { submitInput } from './submitInput.ts';

test('that an input in a chosen life area should result into an entry and a task of this area', async ({
  page,
}, testInfo) => {
  // given
  const marker = `[e2e-area-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  await page.getByRole('textbox', { name: 'Eingabe' }).fill(`Prepare the review ${marker}`);
  await page
    .getByRole('group', { name: 'Lebensbereich', exact: true })
    .getByRole('button', { name: 'Beruflich' })
    .click();

  // when
  await page.getByRole('button', { name: 'Absenden' }).click();

  // then
  const entry = page.getByRole('listitem').filter({ hasText: marker });
  await expect(entry.getByText('Analysiert', { exact: true })).toBeVisible();
  await expect(entry.getByRole('img', { name: 'Beruflich' })).toBeVisible();
});

test('that choosing a life area should result into lists with the elements of this area only', async ({
  page,
  isMobile,
}, testInfo) => {
  // given
  // The AI stub copies the marker into the title; both tasks share its beginning.
  const marker = `e2e-filter-${testInfo.project.name}-${String(Date.now())}`;
  await signIn(page);
  await openNavigation(page, isMobile);
  await page.getByRole('link', { name: 'Aufgaben' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Aufgaben' })).toBeVisible();
  await submitInput(page, `Buy flowers [${marker}-personal]`, 'PERSONAL');
  await submitInput(page, `Book the meeting room [${marker}-work]`, 'WORK');
  const tasks = page.getByRole('listitem').filter({ hasText: marker });
  // Both tasks appear through the push signal of luna-task while all areas are shown.
  await expect(tasks).toHaveCount(2);

  // when
  await page
    .getByRole('group', { name: 'Angezeigter Lebensbereich' })
    .getByRole('button', { name: 'Beruflich' })
    .click();

  // then
  await expect(tasks).toHaveCount(1);
  await expect(tasks.getByText(`[${marker}-work]`)).toBeVisible();
  await expect(tasks.getByRole('img', { name: 'Beruflich' })).toBeVisible();

  // when
  await page.reload();

  // then
  // The chosen area is remembered in the browser.
  await expect(
    page.getByRole('group', { name: 'Angezeigter Lebensbereich' }).getByRole('button', { name: 'Beruflich' }),
  ).toHaveAttribute('aria-pressed', 'true');
  await expect(tasks).toHaveCount(1);
});
