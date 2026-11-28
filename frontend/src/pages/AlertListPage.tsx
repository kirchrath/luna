import { ChevronRight, Eye, Server } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

import type { AlertListItemDto } from '../api';
import { alertPath } from '../navigation';
import { useStores, type AlertFilter } from '../stores';
import { RelativeTime, SegmentedControl } from '../ui';
import { SeverityTag } from './SeverityTag';
import { LifeAreaIcon } from './LifeArea';

const ICON_SIZE = 14;
const CHEVRON_SIZE = 18;
const FILTERS: readonly AlertFilter[] = ['ACTIVE', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'ALL'];

export const AlertListPage = observer(function AlertListPage() {
  const { t } = useTranslation();
  const { alertListStore: store } = useStores();

  useEffect(() => {
    void store.loadInitial();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  return (
    <section className="page">
      <h1 className="title">{t('alerts.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('alerts.lead')}</p>
      <div className="element-list-toolbar">
        <SegmentedControl
          label={t('alerts.filterLabel')}
          options={FILTERS.map((filter) => ({ value: filter, label: t(`alerts.filter.${filter}`) }))}
          value={store.filter}
          onChange={(filter) => void store.changeFilter(filter)}
        />
        <span className="is-size-7 has-text-grey">
          {t(`alerts.count.${store.filter}`, { count: store.totalCount })}
        </span>
      </div>
      <section className="box element-list" aria-label={t('alerts.title')}>
        {store.connection === 'disconnected' && (
          <p className="notification is-warning is-light is-size-7" role="status">
            {t('alerts.liveInterrupted')}
          </p>
        )}
        {store.actionError && (
          <p className="notification is-danger is-light" role="alert">
            {t('alerts.actionFailed')}
          </p>
        )}
        {store.loadError && (
          <div className="notification is-danger is-light" role="alert">
            <p>{t('alerts.loadError')}</p>
            <button type="button" className="button is-small" onClick={() => void store.refresh()}>
              {t('alerts.retry')}
            </button>
          </div>
        )}
        {store.items.length === 0 && !store.loading && !store.loadError && (
          <p className="has-text-grey">{t(`alerts.empty.${store.filter}`)}</p>
        )}
        <ul className="elements">
          {store.items.map((item) => (
            <AlertListEntry key={item.id} item={item} />
          ))}
        </ul>
        {store.hasMore && (
          <button
            type="button"
            className={store.loading ? 'button is-fullwidth is-loading' : 'button is-fullwidth'}
            onClick={() => void store.loadMore()}
          >
            {t('alerts.loadMore')}
          </button>
        )}
      </section>
    </section>
  );
});

const AlertListEntry = observer(function AlertListEntry({ item }: { item: AlertListItemDto }) {
  const { t, i18n } = useTranslation();
  const { alertListStore: store } = useStores();

  return (
    <li className="element alert-entry">
      <Link className="element-link" to={alertPath(item.id)}>
        <SeverityTag severity={item.severity} />
        <span className="element-body">
          <span className="element-title">{item.title}</span>
          <span className="element-meta has-text-grey">
            <LifeAreaIcon area={item.area} />
            <Server size={ICON_SIZE} aria-hidden="true" />
            {item.affectedSystem !== undefined && <span>{item.affectedSystem} ·</span>}
            <RelativeTime dateTime={item.createdDate} locale={i18n.language} />
          </span>
        </span>
        <span className={item.status === 'OPEN' ? 'tag is-rounded' : 'tag is-rounded status-tag-muted'}>
          {t(`alerts.status.${item.status}`)}
        </span>
      </Link>
      {item.status === 'OPEN' && (
        <button
          type="button"
          className={store.isPending(item.id) ? 'button is-small is-loading' : 'button is-small'}
          aria-label={t('alerts.acknowledgeItem', { title: item.title })}
          disabled={store.isPending(item.id)}
          onClick={() => void store.acknowledge(item)}
        >
          <Eye size={ICON_SIZE} aria-hidden="true" />
          <span>{t('alerts.acknowledge')}</span>
        </button>
      )}
      <ChevronRight className="has-text-grey" size={CHEVRON_SIZE} aria-hidden="true" />
    </li>
  );
});
