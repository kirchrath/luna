export const STORAGE_KEYS = {
  language: 'luna.language',
  navigationCollapsed: 'luna.navigation.collapsed',
  appointmentView: 'luna.appointments.view',
} as const;

export type StorageKey = (typeof STORAGE_KEYS)[keyof typeof STORAGE_KEYS];

// Storage can be unavailable (private mode, blocked site data); Luna then simply forgets the preference.
export function readPreference(key: StorageKey): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function writePreference(key: StorageKey, value: string): void {
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // ignored, see readPreference
  }
}
