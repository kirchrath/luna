import { Check, ChevronRight, Clock, Flag } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

import type { TaskListItemDto } from '../api';
import { taskPath } from '../navigation';
import { ALL_TASKS, useStores, type TaskFilter } from '../stores';
import { SegmentedControl } from '../ui';
import { formatDueDate, isOverdue } from './taskDates';

const ICON_SIZE = 14;
const CHEVRON_SIZE = 18;
const FILTERS: readonly TaskFilter[] = ['OPEN', 'DONE', ALL_TASKS];

export const TaskListPage = observer(function TaskListPage() {
  const { t } = useTranslation();
  const { taskListStore: store } = useStores();

  useEffect(() => {
    void store.loadInitial();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  return (
    <section className="page">
      <h1 className="title">{t('tasks.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('tasks.lead')}</p>
      <div className="task-list-toolbar">
        <SegmentedControl
          label={t('tasks.filterLabel')}
          options={FILTERS.map((filter) => ({ value: filter, label: t(`tasks.filter.${filter}`) }))}
          value={store.filter}
          onChange={(filter) => void store.changeFilter(filter)}
        />
        <span className="is-size-7 has-text-grey">{t(`tasks.count.${store.filter}`, { count: store.totalCount })}</span>
      </div>
      <section className="box task-list" aria-label={t('tasks.title')}>
        {store.connection === 'disconnected' && (
          <p className="notification is-warning is-light is-size-7" role="status">
            {t('tasks.liveInterrupted')}
          </p>
        )}
        {store.actionError && (
          <p className="notification is-danger is-light" role="alert">
            {t('tasks.actionFailed')}
          </p>
        )}
        {store.loadError && (
          <div className="notification is-danger is-light" role="alert">
            <p>{t('tasks.loadError')}</p>
            <button type="button" className="button is-small" onClick={() => void store.refresh()}>
              {t('tasks.retry')}
            </button>
          </div>
        )}
        {store.items.length === 0 && !store.loading && !store.loadError && (
          <p className="has-text-grey">{t(`tasks.empty.${store.filter}`)}</p>
        )}
        <ul className="tasks">
          {store.items.map((item) => (
            <TaskListEntry key={item.id} item={item} />
          ))}
        </ul>
        {store.hasMore && (
          <button
            type="button"
            className={store.loading ? 'button is-fullwidth is-loading' : 'button is-fullwidth'}
            onClick={() => void store.loadMore()}
          >
            {t('tasks.loadMore')}
          </button>
        )}
      </section>
    </section>
  );
});

const TaskListEntry = observer(function TaskListEntry({ item }: { item: TaskListItemDto }) {
  const { t, i18n } = useTranslation();
  const { taskListStore: store } = useStores();
  const done = item.status === 'DONE';
  const now = new Date();
  const overdue = !done && item.dueDate !== undefined && isOverdue(item.dueDate, now);
  const due =
    item.dueDate === undefined
      ? t('tasks.noDue')
      : t(overdue ? 'tasks.overdue' : 'tasks.due', { date: formatDueDate(item.dueDate, i18n.language, now) });

  return (
    <li className={done ? 'task is-done' : 'task'}>
      <button
        type="button"
        className={store.isPending(item.id) ? 'task-check is-loading' : 'task-check'}
        role="checkbox"
        aria-checked={done}
        aria-label={t(done ? 'tasks.reopen' : 'tasks.complete', { title: item.title })}
        disabled={store.isPending(item.id)}
        onClick={() => void store.toggle(item)}
      >
        {done && <Check size={ICON_SIZE} aria-hidden="true" />}
      </button>
      <Link className="task-link" to={taskPath(item.id)}>
        <span className="task-body">
          <span className="task-title">{item.title}</span>
          <span className={overdue ? 'task-due is-overdue' : 'task-due has-text-grey'}>
            <Clock size={ICON_SIZE} aria-hidden="true" />
            <span>{due}</span>
            {item.edited && <span className="has-text-grey has-text-weight-normal">· {t('tasks.edited')}</span>}
          </span>
        </span>
        <span className={item.priority === 'HIGH' ? 'tag is-rounded is-link is-light' : 'tag is-rounded'}>
          {item.priority === 'HIGH' && <Flag size={ICON_SIZE - 2} aria-hidden="true" />}
          <span>{t(`tasks.priority.${item.priority}`)}</span>
        </span>
        <ChevronRight className="has-text-grey" size={CHEVRON_SIZE} aria-hidden="true" />
      </Link>
    </li>
  );
});
