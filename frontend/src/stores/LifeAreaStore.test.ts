import { afterEach, describe, expect, it } from 'vitest';

import { STORAGE_KEYS } from '../services';
import { ALL_LIFE_AREAS, LifeAreaStore } from './LifeAreaStore';

describe('LifeAreaStore', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('that a new store without preferences should result into all areas and personal inputs', () => {
    // when
    const store = new LifeAreaStore();

    // then
    expect(store.filter).toBe(ALL_LIFE_AREAS);
    expect(store.inputArea).toBe('PERSONAL');
    expect(store.query).toEqual({});
  });

  it('that remembered preferences should result into the remembered filter and input area', () => {
    // given
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaFilter, 'WORK');
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaInput, 'WORK');

    // when
    const store = new LifeAreaStore();

    // then
    expect(store.filter).toBe('WORK');
    expect(store.lastInputArea).toBe('WORK');
    expect(store.query).toEqual({ area: 'WORK' });
  });

  it('that unknown preferences should result into the defaults', () => {
    // given
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaFilter, 'FAMILY');
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaInput, 'FAMILY');

    // when
    const store = new LifeAreaStore();

    // then
    expect(store.filter).toBe(ALL_LIFE_AREAS);
    expect(store.lastInputArea).toBe('PERSONAL');
  });

  it('that changing the filter should result into the shown area as input area and a remembered filter', () => {
    // given
    const store = new LifeAreaStore();

    // when
    store.changeFilter('WORK');

    // then
    expect(store.filter).toBe('WORK');
    expect(store.inputArea).toBe('WORK');
    expect(window.localStorage.getItem(STORAGE_KEYS.lifeAreaFilter)).toBe('WORK');
  });

  it('that changing the input area while all areas are shown should result into the remembered input area', () => {
    // given
    const store = new LifeAreaStore();

    // when
    store.changeInputArea('WORK');

    // then
    expect(store.inputArea).toBe('WORK');
    expect(window.localStorage.getItem(STORAGE_KEYS.lifeAreaInput)).toBe('WORK');
  });
});
