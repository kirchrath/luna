import { expect, test } from '@playwright/test';

import { openNavigation, signIn } from './signIn.ts';
import { submitInput } from './submitInput.ts';

test('that an appointment input should result into an appointment that can be edited and cancelled', async ({
  page,
  isMobile,
}, testInfo) => {
  // given
  const marker = `[e2e-appointment-${testInfo.project.name}-${String(Date.now())}]`;
  await signIn(page);
  await openNavigation(page, isMobile);
  await page.getByRole('link', { name: 'Termine' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Termine' })).toBeVisible();

  // when
  await submitInput(page, `Dentist tomorrow at ten ${marker}`);

  // then
  // The appointment appears through the push signal of luna-appointment; the list is never polled.
  const appointment = page.getByRole('listitem').filter({ hasText: marker });
  await expect(appointment).toBeVisible();
  await expect(appointment.getByText('Practice at the market')).toBeVisible();

  // when
  await appointment.getByRole('link').click();
  await page.getByLabel('Ort').fill('Practice at the station');
  await page.getByRole('button', { name: 'Speichern' }).click();

  // then
  await expect(
    page.getByText('Bearbeitet: Neue Analyse-Ergebnisse überschreiben deine Änderungen nicht mehr.'),
  ).toBeVisible();

  // when
  await page.getByRole('button', { name: 'Absagen' }).click();

  // then
  await expect(page.getByRole('button', { name: 'Wiederherstellen' })).toBeVisible();

  // when
  await page.getByRole('main').getByRole('link', { name: 'Termine' }).click();

  // then
  const cancelled = page.getByRole('listitem').filter({ hasText: marker });
  await expect(cancelled).toHaveClass(/is-cancelled/);
  await expect(cancelled.getByText('abgesagt')).toBeVisible();
  await expect(cancelled.getByText('Practice at the station')).toBeVisible();
});

test('that the month view should result into the appointment on its day', async ({ page, isMobile }, testInfo) => {
  // given
  const marker = `[e2e-appointment-${testInfo.project.name}-month-${String(Date.now())}]`;
  await signIn(page, '/calendar');
  await page.getByRole('button', { name: 'Monat' }).click();
  await expect(page.getByRole('grid')).toBeVisible();
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
  // The stub schedules the appointment for tomorrow, which may already be in the next month.
  if (tomorrow.getMonth() !== new Date().getMonth()) {
    await page.getByRole('button', { name: 'Nächster Monat' }).click();
  }

  // when
  await submitInput(page, `Dentist tomorrow at ten ${marker}`);

  // then
  // Phones show a dot instead of the title, so the appointment is checked in the list of the chosen day.
  const event = page
    .getByRole('grid')
    .getByRole('link')
    .filter({ has: page.getByText(marker) });
  await expect(event).toHaveCount(1);
  if (!isMobile) {
    await expect(event).toBeVisible();
  }

  // when
  await page
    .getByRole('grid')
    .getByRole('button', { name: new RegExp(`\\b${String(tomorrow.getDate())}\\. `) })
    .click();

  // then
  await expect(page.getByRole('region').getByRole('listitem').filter({ hasText: marker })).toBeVisible();
});
