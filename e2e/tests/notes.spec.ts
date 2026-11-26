import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';
import { submitInput } from './submitInput.ts';

test('that a note input should result into a note that can be edited and archived', async ({
  page,
  isMobile,
}, testInfo) => {
  // given
  const marker = `[e2e-note-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  await openNavigation(page, isMobile);
  await page.getByRole('link', { name: 'Notizen' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Notizen' })).toBeVisible();

  // when
  await submitInput(page, `Minutes of the owners meeting ${marker}`);

  // then
  // The note appears through the push signal of luna-note; the list is never polled.
  const note = page.getByRole('listitem').filter({ hasText: marker });
  await expect(note).toBeVisible();
  await expect(note.getByText('Roof will be renovated in May')).toBeVisible();

  // when
  await note.getByRole('link').click();
  await page.getByLabel('Zusammenfassung').fill('Roof in May, next meeting in October');
  await page.getByRole('button', { name: 'Speichern' }).click();

  // then
  await expect(
    page.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
  ).toBeVisible();

  // when
  await page.getByRole('button', { name: 'Archivieren' }).click();

  // then
  await expect(page.getByRole('button', { name: 'Wiederherstellen' })).toBeVisible();

  // when
  await page.getByRole('main').getByRole('link', { name: 'Notizen' }).click();
  await page.getByRole('button', { name: 'Archiviert' }).click();

  // then
  const archived = page.getByRole('listitem').filter({ hasText: marker });
  await expect(archived).toBeVisible();
  await expect(archived.getByText('Roof in May, next meeting in October')).toBeVisible();
});
