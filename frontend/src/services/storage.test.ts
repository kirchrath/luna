import { afterEach, describe, expect, it, vi } from 'vitest';

import { readPreference, STORAGE_KEYS, writePreference } from './storage';

describe('storage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('that writing a preference should result into reading the same value', () => {
    // given
    const value = 'en';

    // when
    writePreference(STORAGE_KEYS.language, value);

    // then
    expect(readPreference(STORAGE_KEYS.language)).toBe(value);
  });

  it('that unavailable storage should result into no preference and no error', () => {
    // given
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });

    // when
    const write = () => {
      writePreference(STORAGE_KEYS.language, 'en');
    };

    // then
    expect(write).not.toThrow();
    expect(readPreference(STORAGE_KEYS.language)).toBeNull();
  });
});
