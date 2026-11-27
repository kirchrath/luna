import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';

import { LIFE_AREA_ICONS } from '../lifeAreas';
import { LIFE_AREAS, useStores } from '../stores';

const ICON_SIZE = 16;

/** Shows or hides the life areas on this device; the last visible area cannot be hidden. */
export const LifeAreaVisibility = observer(function LifeAreaVisibility() {
  const { t } = useTranslation();
  const { lifeAreaStore } = useStores();
  return (
    <fieldset className="dropdown-item life-area-visibility">
      <legend className="has-text-weight-semibold is-size-7">{t('lifeArea.deviceLabel')}</legend>
      {LIFE_AREAS.map((area) => {
        const Icon = LIFE_AREA_ICONS[area];
        const visible = lifeAreaStore.isVisible(area);
        const lastVisible = visible && !lifeAreaStore.choosable;
        return (
          <label key={area} className="checkbox" title={lastVisible ? t('lifeArea.lastVisible') : undefined}>
            <input
              type="checkbox"
              checked={visible}
              disabled={lastVisible}
              onChange={(event) => {
                lifeAreaStore.changeVisibility(area, event.target.checked);
              }}
            />
            <Icon size={ICON_SIZE} aria-hidden="true" />
            <span>{t(`lifeArea.${area}`)}</span>
          </label>
        );
      })}
    </fieldset>
  );
});
