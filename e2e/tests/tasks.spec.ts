import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';
import { submitInput } from './submitInput.ts';

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
