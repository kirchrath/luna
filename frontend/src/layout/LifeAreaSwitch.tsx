import { observer } from 'mobx-react-lite';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { ALL_LIFE_AREAS_ICON, LIFE_AREA_ICONS } from '../lifeAreas';
import { ALL_LIFE_AREAS, LIFE_AREAS, useStores, type LifeAreaFilter } from '../stores';
import { SegmentedControl } from '../ui';

const ICON_SIZE = 16;

/** Chooses the life area the whole app shows. */
export const LifeAreaSwitch = observer(function LifeAreaSwitch() {
  const { t } = useTranslation();
  const { lifeAreaStore } = useStores();
  const AllIcon = ALL_LIFE_AREAS_ICON;
  const options: { value: LifeAreaFilter; label: string; icon: ReactNode }[] = [
    { value: ALL_LIFE_AREAS, label: t('lifeArea.ALL'), icon: <AllIcon size={ICON_SIZE} aria-hidden="true" /> },
    ...LIFE_AREAS.map((area) => {
      const Icon = LIFE_AREA_ICONS[area];
      return { value: area, label: t(`lifeArea.${area}`), icon: <Icon size={ICON_SIZE} aria-hidden="true" /> };
    }),
  ];
  return (
    <SegmentedControl
      className="life-area-switch"
      label={t('lifeArea.filterLabel')}
      options={options}
      value={lifeAreaStore.filter}
      onChange={(filter) => {
        lifeAreaStore.changeFilter(filter);
      }}
    />
  );
});
