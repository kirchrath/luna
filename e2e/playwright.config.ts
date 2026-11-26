import { defineConfig, devices } from '@playwright/test';

import { BASE_URL } from './environment/settings.ts';

// Only needed where the browser of this Playwright version cannot be installed (e.g. restricted networks).
const executablePath = process.env['E2E_CHROMIUM_EXECUTABLE'];

export default defineConfig({
  testDir: './tests',
  globalSetup: './environment/global-setup.ts',
  globalTeardown: './environment/global-teardown.ts',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    trace: 'off',
    ...(executablePath === undefined ? {} : { launchOptions: { executablePath } }),
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
});
