import { useTranslation } from 'react-i18next';

import type { AreaDefinition } from '../navigation';
import { Placeholder } from '../ui/Placeholder';

const ICON_SIZE = 40;

export function ComingSoonPage({ area, icon: Icon }: AreaDefinition) {
  const { t } = useTranslation();
  return (
    <Placeholder
      icon={<Icon size={ICON_SIZE} aria-hidden="true" />}
      title={t(`navigation.${area}`)}
      text={t('comingSoon.text')}
    />
  );
}
