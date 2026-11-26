import { ChevronRight, StickyNote } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

import type { NoteListItemDto } from '../api';
import { notePath } from '../navigation';
import { ALL_NOTES, useStores, type NoteFilter } from '../stores';
import { RelativeTime, SegmentedControl } from '../ui';

const ICON_SIZE = 20;
const CHEVRON_SIZE = 18;
const FILTERS: readonly NoteFilter[] = ['ACTIVE', 'ARCHIVED', ALL_NOTES];

export const NoteListPage = observer(function NoteListPage() {
  const { t } = useTranslation();
  const { noteListStore: store } = useStores();

  useEffect(() => {
    void store.loadInitial();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  return (
    <section className="page">
      <h1 className="title">{t('notes.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('notes.lead')}</p>
      <div className="element-list-toolbar">
        <SegmentedControl
          label={t('notes.filterLabel')}
          options={FILTERS.map((filter) => ({ value: filter, label: t(`notes.filter.${filter}`) }))}
          value={store.filter}
          onChange={(filter) => void store.changeFilter(filter)}
        />
        <span className="is-size-7 has-text-grey">{t(`notes.count.${store.filter}`, { count: store.totalCount })}</span>
      </div>
      <section className="box element-list" aria-label={t('notes.title')}>
        {store.connection === 'disconnected' && (
          <p className="notification is-warning is-light is-size-7" role="status">
            {t('notes.liveInterrupted')}
          </p>
        )}
        {store.loadError && (
          <div className="notification is-danger is-light" role="alert">
            <p>{t('notes.loadError')}</p>
            <button type="button" className="button is-small" onClick={() => void store.refresh()}>
              {t('notes.retry')}
            </button>
          </div>
        )}
        {store.items.length === 0 && !store.loading && !store.loadError && (
          <p className="has-text-grey">{t(`notes.empty.${store.filter}`)}</p>
        )}
        <ul className="elements">
          {store.items.map((item) => (
            <NoteListEntry key={item.id} item={item} />
          ))}
        </ul>
        {store.hasMore && (
          <button
            type="button"
            className={store.loading ? 'button is-fullwidth is-loading' : 'button is-fullwidth'}
            onClick={() => void store.loadMore()}
          >
            {t('notes.loadMore')}
          </button>
        )}
      </section>
    </section>
  );
});

const NoteListEntry = observer(function NoteListEntry({ item }: { item: NoteListItemDto }) {
  const { t, i18n } = useTranslation();

  return (
    <li className={item.status === 'ARCHIVED' ? 'element is-archived' : 'element'}>
      <Link className="element-link" to={notePath(item.id)}>
        <StickyNote className="has-text-grey" size={ICON_SIZE} aria-hidden="true" />
        <span className="element-body">
          <span className="element-title">{item.title}</span>
          <span className="element-summary has-text-grey">{item.summary}</span>
          <span className="element-meta has-text-grey">
            <RelativeTime dateTime={item.createdDate} locale={i18n.language} />
            {item.edited && <span>· {t('notes.edited')}</span>}
          </span>
        </span>
        <ChevronRight className="has-text-grey" size={CHEVRON_SIZE} aria-hidden="true" />
      </Link>
    </li>
  );
});
