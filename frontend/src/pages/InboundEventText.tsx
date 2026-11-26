import { Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { InboundEventListItemDto } from '../api';

const ICON_SIZE = 15;

/** Manual inputs show their text, emails their subject and sender. */
export function InboundEventText({ item }: { item: InboundEventListItemDto }) {
  const { t } = useTranslation();
  if (item.source === 'MANUAL') {
    return (
      <span className="inbound-event-text" title={item.text}>
        {item.text}
      </span>
    );
  }
  const subject = item.subject ?? t('quickInput.noSubject');
  return (
    <span className="inbound-event-text inbound-event-mail" title={subject}>
      <Mail className="has-text-grey" size={ICON_SIZE} aria-label={t('quickInput.source.MAIL')} role="img" />
      <span className="inbound-event-mail-body">
        <span className="has-text-weight-semibold">{subject}</span>
        {item.from !== undefined && <span className="is-size-7 has-text-grey">{item.from}</span>}
      </span>
    </span>
  );
}
