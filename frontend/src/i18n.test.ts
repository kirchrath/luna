import i18n from 'i18next';
import { describe, expect, it } from 'vitest';

import { changeLanguage, initI18n, initialLanguage } from './i18n';
import { readPreference, STORAGE_KEYS, writePreference } from './services/storage';

describe('i18n', () => {
  it('that starting without a stored language should result into German', () => {
    // given
    window.localStorage.clear();

    // when
    const language = initialLanguage();

    // then
    expect(language).toBe('de');
  });

  it('that starting with a stored supported language should result into that language', async () => {
    // given
    writePreference(STORAGE_KEYS.language, 'en');

    // when
    await initI18n();

    // then
    expect(i18n.language).toBe('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('that starting with a stored unsupported language should result into German', () => {
    // given
    writePreference(STORAGE_KEYS.language, 'fr');

    // when
    const language = initialLanguage();

    // then
    expect(language).toBe('de');
  });

  it('that changing the language should result into a stored and applied language', async () => {
    // given
    await initI18n();

    // when
    await changeLanguage('en');

    // then
    expect(i18n.language).toBe('en');
    expect(document.documentElement.lang).toBe('en');
    expect(readPreference(STORAGE_KEYS.language)).toBe('en');
  });
});
