import { makeAutoObservable } from 'mobx';

import type { LifeAreaDto } from '../api';
import { readPreference, STORAGE_KEYS, writePreference } from '../services';

export const LIFE_AREAS: readonly LifeAreaDto[] = ['PERSONAL', 'WORK'];
export const ALL_LIFE_AREAS = 'ALL';
export type LifeAreaFilter = LifeAreaDto | typeof ALL_LIFE_AREAS;
const DEFAULT_INPUT_AREA: LifeAreaDto = 'PERSONAL';
const SEPARATOR = ',';

function isLifeArea(value: string | null): value is LifeAreaDto {
  return LIFE_AREAS.some((area) => area === value);
}

/**
 * The life areas of this device: which areas are hidden here, which one the whole app shows (all visible areas or
 * one of them) and the area of new inputs. Everything is remembered in the browser; the list stores reload when
 * the shown areas change.
 */
export class LifeAreaStore {
  private _chosenFilter: LifeAreaFilter;
  private _lastInputArea: LifeAreaDto;
  private _hiddenAreas: LifeAreaDto[];
  private readonly areas: readonly LifeAreaDto[];

  /** @param areas all life areas; only tests pass more to cover rules for more than two areas */
  constructor(areas: readonly LifeAreaDto[] = LIFE_AREAS) {
    this.areas = areas;
    const filter = readPreference(STORAGE_KEYS.lifeAreaFilter);
    const inputArea = readPreference(STORAGE_KEYS.lifeAreaInput);
    const hidden = (readPreference(STORAGE_KEYS.lifeAreaHidden) ?? '').split(SEPARATOR);
    this._chosenFilter = isLifeArea(filter) ? filter : ALL_LIFE_AREAS;
    this._lastInputArea = isLifeArea(inputArea) ? inputArea : DEFAULT_INPUT_AREA;
    const hiddenAreas = areas.filter((area) => hidden.includes(area));
    // At least one area always stays visible.
    this._hiddenAreas = hiddenAreas.length === areas.length ? [] : hiddenAreas;
    makeAutoObservable<LifeAreaStore, 'areas'>(this, { areas: false });
  }

  private get chosenFilter(): LifeAreaFilter {
    return this._chosenFilter;
  }

  private set chosenFilter(filter: LifeAreaFilter) {
    this._chosenFilter = filter;
  }

  get lastInputArea(): LifeAreaDto {
    return this._lastInputArea;
  }

  private set lastInputArea(area: LifeAreaDto) {
    this._lastInputArea = area;
  }

  get hiddenAreas(): LifeAreaDto[] {
    return this._hiddenAreas;
  }

  private set hiddenAreas(areas: LifeAreaDto[]) {
    this._hiddenAreas = areas;
  }

  get visibleAreas(): LifeAreaDto[] {
    return this.areas.filter((area) => !this.hiddenAreas.includes(area));
  }

  /** Only with more than one visible area there is anything to choose. */
  get choosable(): boolean {
    return this.visibleAreas.length > 1;
  }

  /** The shown area: all visible areas or one of them, never a hidden one. */
  get filter(): LifeAreaFilter {
    const [onlyArea] = this.visibleAreas;
    if (!this.choosable && onlyArea !== undefined) {
      return onlyArea;
    }
    return this.chosenFilter !== ALL_LIFE_AREAS && this.hiddenAreas.includes(this.chosenFilter)
      ? ALL_LIFE_AREAS
      : this.chosenFilter;
  }

  /** New inputs belong to the shown area; while all areas are shown, to the area chosen last. */
  get inputArea(): LifeAreaDto {
    return this.filter === ALL_LIFE_AREAS ? this.lastInputArea : this.filter;
  }

  /** The query parameter of list requests: none while every area is shown. */
  get query(): { area?: LifeAreaDto[] } {
    if (this.filter !== ALL_LIFE_AREAS) {
      return { area: [this.filter] };
    }
    return this.hiddenAreas.length === 0 ? {} : { area: this.visibleAreas };
  }

  isVisible(area: LifeAreaDto): boolean {
    return !this.hiddenAreas.includes(area);
  }

  changeFilter(filter: LifeAreaFilter): void {
    this.chosenFilter = filter;
    writePreference(STORAGE_KEYS.lifeAreaFilter, filter);
  }

  changeInputArea(area: LifeAreaDto): void {
    this.lastInputArea = area;
    writePreference(STORAGE_KEYS.lifeAreaInput, area);
  }

  /** Shows or hides an area on this device; the last visible area cannot be hidden. */
  changeVisibility(area: LifeAreaDto, visible: boolean): void {
    if (!visible && this.visibleAreas.length === 1 && this.isVisible(area)) {
      return;
    }
    this.hiddenAreas = this.areas.filter((hidden) => (hidden === area ? !visible : this.hiddenAreas.includes(hidden)));
    writePreference(STORAGE_KEYS.lifeAreaHidden, this.hiddenAreas.join(SEPARATOR));
  }
}
