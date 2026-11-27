import { describe, expect, it } from 'vitest';

import type { LifeAreaDto } from '../api';
import { STORAGE_KEYS } from '../services';
import { ALL_LIFE_AREAS, LifeAreaStore } from './LifeAreaStore';

describe('LifeAreaStore', () => {
  it('that a new store without preferences should result into all areas and personal inputs', () => {
    // when
    const store = new LifeAreaStore();

    // then
    expect(store.filter).toBe(ALL_LIFE_AREAS);
    expect(store.inputArea).toBe('PERSONAL');
    expect(store.query).toEqual({});
    expect(store.visibleAreas).toEqual(['PERSONAL', 'WORK']);
    expect(store.choosable).toBe(true);
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
    expect(store.query).toEqual({ area: ['WORK'] });
  });

  it('that unknown preferences should result into the defaults', () => {
    // given
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaFilter, 'FAMILY');
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaInput, 'FAMILY');
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaHidden, 'FAMILY');

    // when
    const store = new LifeAreaStore();

    // then
    expect(store.filter).toBe(ALL_LIFE_AREAS);
    expect(store.lastInputArea).toBe('PERSONAL');
    expect(store.hiddenAreas).toEqual([]);
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

  it('that hiding an area should result into only the other area everywhere and a remembered choice', () => {
    // given
    const store = new LifeAreaStore();
    store.changeInputArea('WORK');

    // when
    store.changeVisibility('WORK', false);

    // then
    expect(store.isVisible('WORK')).toBe(false);
    expect(store.visibleAreas).toEqual(['PERSONAL']);
    expect(store.choosable).toBe(false);
    expect(store.filter).toBe('PERSONAL');
    expect(store.inputArea).toBe('PERSONAL');
    expect(store.query).toEqual({ area: ['PERSONAL'] });
    expect(window.localStorage.getItem(STORAGE_KEYS.lifeAreaHidden)).toBe('WORK');
  });

  it('that hiding the last visible area should result into no change', () => {
    // given
    const store = new LifeAreaStore();
    store.changeVisibility('WORK', false);

    // when
    store.changeVisibility('PERSONAL', false);

    // then
    expect(store.visibleAreas).toEqual(['PERSONAL']);
  });

  it('that showing a hidden area again should result into all areas and the remembered filter', () => {
    // given
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaHidden, 'PERSONAL');
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaFilter, 'PERSONAL');
    const store = new LifeAreaStore();
    const filterWhileHidden = store.filter;

    // when
    store.changeVisibility('PERSONAL', true);

    // then
    expect(filterWhileHidden).toBe('WORK');
    expect(store.hiddenAreas).toEqual([]);
    expect(store.filter).toBe('PERSONAL');
    expect(window.localStorage.getItem(STORAGE_KEYS.lifeAreaHidden)).toBe('');
  });

  it('that a remembered setting hiding every area should result into all areas visible', () => {
    // given
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaHidden, 'PERSONAL,WORK');

    // when
    const store = new LifeAreaStore();

    // then
    expect(store.visibleAreas).toEqual(['PERSONAL', 'WORK']);
  });

  it('that hiding one of three areas should result into the two others and a hidden chosen area showing both', () => {
    // given
    const family = 'FAMILY' as LifeAreaDto;
    window.localStorage.setItem(STORAGE_KEYS.lifeAreaFilter, 'WORK');
    const store = new LifeAreaStore(['PERSONAL', 'WORK', family]);

    // when
    store.changeVisibility('WORK', false);

    // then
    expect(store.choosable).toBe(true);
    expect(store.filter).toBe(ALL_LIFE_AREAS);
    expect(store.query).toEqual({ area: ['PERSONAL', family] });
  });
});
