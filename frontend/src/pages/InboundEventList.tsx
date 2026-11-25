import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';

import type { InboundEventStatusDto } from '../api';
import type { InboundEventStore } from '../stores';
import { RelativeTime } from '../ui';
import { ELEMENT_TYPE_ICONS } from './elementTypes';

const TAG_ICON_SIZE = 13;
const STATUS_CLASSES: Record<InboundEventStatusDto, string> = {
  RECEIVED: 'is-info is-light',
  ANALYZED: 'is-success is-light',
  FAILED: 'is-danger is-light',
};

interface InboundEventListProps {
  store: InboundEventStore;
}

export const InboundEventList = observer(function InboundEventList({ store }: InboundEventListProps) {
  const { t, i18n } = useTranslation();

  return (
    <section className="box inbound-event-list" aria-labelledby="inbound-event-list-title">
      <h2 id="inbound-event-list-title" className="title is-5">
        {t('quickInput.listTitle')}
      </h2>
      {store.connection === 'disconnected' && (
        <p className="notification is-warning is-light is-size-7" role="status">
          {t('quickInput.liveInterrupted')}
        </p>
      )}
      {store.loadError && (
        <div className="notification is-danger is-light" role="alert">
          <p>{t('quickInput.loadError')}</p>
          <button type="button" className="button is-small" onClick={() => void store.refresh()}>
            {t('quickInput.retry')}
          </button>
        </div>
      )}
      {store.items.length === 0 && !store.loading && !store.loadError && (
        <p className="has-text-grey">{t('quickInput.empty')}</p>
      )}
      <ul className="inbound-events">
        {store.items.map((item) => (
          <li key={item.id} className="inbound-event">
            <span className="inbound-event-text" title={item.text}>
              {item.text}
            </span>
            <span className="inbound-event-time has-text-grey">
              <RelativeTime dateTime={item.createdDate} locale={i18n.language} />
            </span>
            <span className="inbound-event-status">
              <span className={`tag is-rounded ${STATUS_CLASSES[item.status]}`}>
                {t(`quickInput.status.${item.status}`)}
              </span>
              <span className="tags">
                {item.elements.map((element) => {
                  const Icon = ELEMENT_TYPE_ICONS[element.type];
                  return (
                    <span key={element.id} className="tag is-rounded">
                      <Icon size={TAG_ICON_SIZE} aria-hidden="true" />
                      <span>{t(`elementType.${element.type}`)}</span>
                    </span>
                  );
                })}
              </span>
            </span>
          </li>
        ))}
      </ul>
      {store.hasMore && (
        <button
          type="button"
          className={store.loading ? 'button is-fullwidth is-loading' : 'button is-fullwidth'}
          onClick={() => void store.loadMore()}
        >
          {t('quickInput.loadMore')}
        </button>
      )}
    </section>
  );
});
