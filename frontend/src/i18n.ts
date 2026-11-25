import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import de from './locales/de.json';
import en from './locales/en.json';
import { readPreference, STORAGE_KEYS, writePreference } from './services/storage';

export const LANGUAGES = ['de', 'en'] as const;
export type Language = (typeof LANGUAGES)[number];
export const DEFAULT_LANGUAGE: Language = 'de';

function isLanguage(value: string | null): value is Language {
  return LANGUAGES.some((language) => language === value);
}

export function initialLanguage(): Language {
  const stored = readPreference(STORAGE_KEYS.language);
  return isLanguage(stored) ? stored : DEFAULT_LANGUAGE;
}

export async function changeLanguage(language: Language): Promise<void> {
  writePreference(STORAGE_KEYS.language, language);
  document.documentElement.lang = language;
  await i18n.changeLanguage(language);
}

export async function initI18n(): Promise<typeof i18n> {
  const language = initialLanguage();
  document.documentElement.lang = language;
  await i18n.use(initReactI18next).init({
    resources: { de: { translation: de }, en: { translation: en } },
    lng: language,
    fallbackLng: DEFAULT_LANGUAGE,
    interpolation: { escapeValue: false },
  });
  return i18n;
}
