import { TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { AlertSeverityDto } from '../api';

const ICON_SIZE = 12;
/** As planned: critical in the orange of overdue items, warnings in petrol, information grey. */
const SEVERITY_CLASSES: Record<AlertSeverityDto, string> = {
  CRITICAL: 'tag is-rounded severity-tag is-critical',
  WARNING: 'tag is-rounded severity-tag is-link is-light',
  INFO: 'tag is-rounded severity-tag',
};

export function SeverityTag({ severity }: { severity: AlertSeverityDto }) {
  const { t } = useTranslation();
  return (
    <span className={SEVERITY_CLASSES[severity]}>
      {severity !== 'INFO' && <TriangleAlert size={ICON_SIZE} aria-hidden="true" />}
      <span>{t(`alerts.severity.${severity}`)}</span>
    </span>
  );
}
