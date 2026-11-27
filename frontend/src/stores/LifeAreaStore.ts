import { makeAutoObservable } from 'mobx';

import type { LifeAreaDto } from '../api';
import { readPreference, STORAGE_KEYS, writePreference } from '../services';

export const LIFE_AREAS: readonly LifeAreaDto[] = ['PERSONAL', 'WORK'];
export const ALL_LIFE_AREAS = 'ALL';
export type LifeAreaFilter = LifeAreaDto | typeof ALL_LIFE_AREAS;
const DEFAULT_INPUT_AREA: LifeAreaDto = 'PERSONAL';

function isLifeArea(value: string | null): value is LifeAreaDto {
  return LIFE_AREAS.some((area) => area === value);
}

/**
 * The life area the whole app shows (all or one area) and the area of new inputs. Both are remembered in the
 * browser; the list stores reload when the filter changes.
 */
export class LifeAreaStore {
  private _filter: LifeAreaFilter;
  private _lastInputArea: LifeAreaDto;

  constructor() {
    const filter = readPreference(STORAGE_KEYS.lifeAreaFilter);
    const inputArea = readPreference(STORAGE_KEYS.lifeAreaInput);
    this._filter = isLifeArea(filter) ? filter : ALL_LIFE_AREAS;
    this._lastInputArea = isLifeArea(inputArea) ? inputArea : DEFAULT_INPUT_AREA;
    makeAutoObservable(this);
  }

  get filter(): LifeAreaFilter {
    return this._filter;
  }

  private set filter(filter: LifeAreaFilter) {
    this._filter = filter;
  }

  get lastInputArea(): LifeAreaDto {
    return this._lastInputArea;
  }

  private set lastInputArea(area: LifeAreaDto) {
    this._lastInputArea = area;
  }

  /** New inputs belong to the shown area; while all areas are shown, to the area chosen last. */
  get inputArea(): LifeAreaDto {
    return this.filter === ALL_LIFE_AREAS ? this.lastInputArea : this.filter;
  }

  /** The query parameter of list requests: none while all areas are shown. */
  get query(): { area?: LifeAreaDto } {
    return this.filter === ALL_LIFE_AREAS ? {} : { area: this.filter };
  }

  changeFilter(filter: LifeAreaFilter): void {
    this.filter = filter;
    writePreference(STORAGE_KEYS.lifeAreaFilter, filter);
  }

  changeInputArea(area: LifeAreaDto): void {
    this.lastInputArea = area;
    writePreference(STORAGE_KEYS.lifeAreaInput, area);
  }
}
