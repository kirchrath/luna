import { useTranslation } from 'react-i18next';

import type { LifeAreaDto } from '../api';
import { LIFE_AREA_ICONS } from '../lifeAreas';

const ICON_SIZE = 14;

/** The life area of an element in a list: a small icon whose name is read out and shown on hover. */
export function LifeAreaIcon({ area }: { area: LifeAreaDto }) {
  const { t } = useTranslation();
  const Icon = LIFE_AREA_ICONS[area];
  const name = t(`lifeArea.${area}`);
  return (
    <span className="life-area-icon has-text-grey" role="img" aria-label={name} title={name}>
      <Icon size={ICON_SIZE} aria-hidden="true" />
    </span>
  );
}

/** The life area of an element on its detail page. */
export function LifeAreaDetail({ area }: { area: LifeAreaDto }) {
  const { t } = useTranslation();
  const Icon = LIFE_AREA_ICONS[area];
  return (
    <p className="element-origin">
      <Icon size={ICON_SIZE} aria-hidden="true" />
      <span>{t('lifeArea.detail', { area: t(`lifeArea.${area}`) })}</span>
    </p>
  );
}
