import { Check, ChevronLeft, RotateCcw, Sparkles } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect, useState, type SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';

import type { TaskDto, TaskPriorityDto } from '../api';
import { PATHS, ID_PARAMETER } from '../navigation';
import { useStores } from '../stores';
import { ChipGroup, RelativeTime } from '../ui';
import { formatDateTime, fromDueDateInput, toDueDateInput } from './taskDates';
import { LifeAreaDetail } from './LifeArea';

export const MAX_TITLE_LENGTH = 500;
export const MAX_DESCRIPTION_LENGTH = 10_000;
const ICON_SIZE = 16;
const PRIORITIES: readonly TaskPriorityDto[] = ['LOW', 'NORMAL', 'HIGH'];
const IDS = {
  title: 'task-title',
  titleHelp: 'task-title-help',
  description: 'task-description',
  dueDate: 'task-due-date',
  dueTime: 'task-due-time',
} as const;

export const TaskDetailPage = observer(function TaskDetailPage() {
  const { t } = useTranslation();
  const { taskStore: store } = useStores();
  // The route always contains the parameter.
  const id = String(useParams()[ID_PARAMETER]);

  useEffect(() => {
    void store.load(id);
  }, [store, id]);

  const task = store.id === id ? store.task : null;

  return (
    <section className="page">
      <Link className="task-back has-text-grey" to={PATHS.tasks}>
        <ChevronLeft size={ICON_SIZE} aria-hidden="true" />
        <span>{t('taskDetail.back')}</span>
      </Link>
      {task === null ? (
        <TaskLoadMessage />
      ) : (
        // A new version (saved, completed or reloaded) resets the form to the stored state.
        <TaskDetail key={task.audit.version} task={task} />
      )}
    </section>
  );
});

const TaskLoadMessage = observer(function TaskLoadMessage() {
  const { t } = useTranslation();
  const { taskStore: store } = useStores();

  if (store.loadState === 'notFound') {
    return (
      <div className="box">
        <h1 className="title is-4">{t('taskDetail.notFound')}</h1>
        <p className="has-text-grey">{t('taskDetail.notFoundText')}</p>
      </div>
    );
  }
  if (store.loadState === 'failed') {
    return (
      <div className="notification is-danger is-light" role="alert">
        <p>{t('taskDetail.loadError')}</p>
        <button type="button" className="button is-small" onClick={() => void store.reload()}>
          {t('taskDetail.retry')}
        </button>
      </div>
    );
  }
  return <progress className="progress is-small is-primary" aria-label={t('taskDetail.formLabel')} />;
});

const TaskDetail = observer(function TaskDetail({ task }: { task: TaskDto }) {
  const { t, i18n } = useTranslation();
  const { taskStore: store } = useStores();
  const { data, audit } = task;
  const done = data.status === 'DONE';
  const StatusIcon = done ? RotateCcw : Check;

  return (
    <>
      <div className="task-heading">
        <h1 className="title">{data.title}</h1>
        <button
          type="button"
          className={
            store.statusChanging ? 'button is-primary is-outlined is-loading' : 'button is-primary is-outlined'
          }
          onClick={() => void (done ? store.reopen() : store.complete())}
        >
          <StatusIcon size={ICON_SIZE} aria-hidden="true" />
          <span>{t(done ? 'taskDetail.reopen' : 'taskDetail.complete')}</span>
        </button>
      </div>
      <p className="task-meta is-size-7 has-text-grey">
        <span className={done ? 'tag is-rounded is-success is-light' : 'tag is-rounded'}>
          {t(`tasks.status.${data.status}`)}
        </span>
        <span>
          {t('taskDetail.version', { version: audit.version })} · {t('taskDetail.changed')}{' '}
          <RelativeTime dateTime={audit.lastModifiedDate} locale={i18n.language} />
        </span>
      </p>
      {store.statusError && (
        <p className="notification is-danger is-light" role="alert">
          {t('taskDetail.statusFailed')}
        </p>
      )}
      {store.saveError === 'conflict' && (
        <div className="notification is-link is-light task-conflict" role="alert">
          <div>
            <p className="has-text-weight-semibold">{t('taskDetail.conflict')}</p>
            <p className="is-size-7">{t('taskDetail.conflictText')}</p>
          </div>
          <button type="button" className="button is-small is-link is-outlined" onClick={() => void store.reload()}>
            {t('taskDetail.reload')}
          </button>
        </div>
      )}
      <TaskForm task={task} />
      <section className="box" aria-labelledby="task-origin-title">
        <h2 id="task-origin-title" className="title is-6">
          {t('taskDetail.origin')}
        </h2>
        <p className="task-origin">
          <Sparkles size={ICON_SIZE} aria-hidden="true" />
          <span>{t(`taskDetail.source.${data.source}`)}</span>
        </p>
        <LifeAreaDetail area={data.area} />
        <p className="is-size-7 has-text-grey">
          {t(data.edited ? 'taskDetail.editedHint' : 'taskDetail.uneditedHint')}
        </p>
        <p className="is-size-7 has-text-grey">
          {t('taskDetail.created', { date: formatDateTime(audit.createdDate, i18n.language) })} ·{' '}
          {t('taskDetail.modified', { date: formatDateTime(audit.lastModifiedDate, i18n.language) })}
          {data.completedDate !== undefined &&
            ` · ${t('taskDetail.completed', { date: formatDateTime(data.completedDate, i18n.language) })}`}
        </p>
      </section>
    </>
  );
});

const TaskForm = observer(function TaskForm({ task }: { task: TaskDto }) {
  const { t } = useTranslation();
  const { taskStore: store } = useStores();
  const initialDue = toDueDateInput(task.data.dueDate);
  const initialDescription = task.data.description ?? '';
  const [title, setTitle] = useState(task.data.title);
  const [description, setDescription] = useState(initialDescription);
  const [dueDate, setDueDate] = useState(initialDue.date);
  const [dueTime, setDueTime] = useState(initialDue.time);
  const [priority, setPriority] = useState<TaskPriorityDto>(task.data.priority);
  const [titleMissing, setTitleMissing] = useState(false);

  const discard = () => {
    setTitle(task.data.title);
    setDescription(initialDescription);
    setDueDate(initialDue.date);
    setDueTime(initialDue.time);
    setPriority(task.data.priority);
    setTitleMissing(false);
  };

  const save = async (event: SyntheticEvent) => {
    event.preventDefault();
    const trimmedTitle = title.trim();
    if (trimmedTitle === '') {
      setTitleMissing(true);
      return;
    }
    const trimmedDescription = description.trim();
    const due = fromDueDateInput({ date: dueDate, time: dueTime });
    await store.save({
      title: trimmedTitle,
      priority,
      ...(trimmedDescription === '' ? {} : { description: trimmedDescription }),
      ...(due === undefined ? {} : { dueDate: due }),
    });
  };

  const titleError = titleMissing ? t('taskDetail.titleMissing') : null;

  return (
    <form
      className="box task-form"
      aria-label={t('taskDetail.formLabel')}
      noValidate
      onSubmit={(event) => void save(event)}
    >
      <div className="field">
        <label className="label" htmlFor={IDS.title}>
          {t('taskDetail.titleLabel')}
        </label>
        <div className="control">
          <input
            id={IDS.title}
            className={titleError === null ? 'input' : 'input is-danger'}
            value={title}
            maxLength={MAX_TITLE_LENGTH}
            aria-invalid={titleError !== null}
            aria-describedby={IDS.titleHelp}
            onChange={(event) => {
              setTitle(event.target.value);
              setTitleMissing(false);
            }}
          />
        </div>
        {titleError !== null && (
          <p id={IDS.titleHelp} className="help is-danger">
            {titleError}
          </p>
        )}
      </div>
      <div className="field">
        <label className="label" htmlFor={IDS.description}>
          {t('taskDetail.descriptionLabel')}
        </label>
        <div className="control">
          <textarea
            id={IDS.description}
            className="textarea"
            rows={3}
            value={description}
            maxLength={MAX_DESCRIPTION_LENGTH}
            onChange={(event) => {
              setDescription(event.target.value);
            }}
          />
        </div>
      </div>
      <div className="columns is-mobile is-multiline task-due">
        <div className="column is-full-mobile">
          <label className="label" htmlFor={IDS.dueDate}>
            {t('taskDetail.dueDateLabel')}
          </label>
          <input
            id={IDS.dueDate}
            className="input"
            type="date"
            value={dueDate}
            onChange={(event) => {
              setDueDate(event.target.value);
              if (event.target.value === '') {
                setDueTime('');
              }
            }}
          />
        </div>
        <div className="column is-full-mobile">
          <label className="label" htmlFor={IDS.dueTime}>
            {t('taskDetail.dueTimeLabel')}
          </label>
          <input
            id={IDS.dueTime}
            className="input"
            type="time"
            value={dueTime}
            disabled={dueDate === ''}
            onChange={(event) => {
              setDueTime(event.target.value);
            }}
          />
        </div>
      </div>
      <div className="field">
        <span className="label">{t('taskDetail.priorityLabel')}</span>
        <ChipGroup
          label={t('taskDetail.priorityLabel')}
          options={PRIORITIES.map((value) => ({ value, label: t(`tasks.priority.${value}`) }))}
          value={priority}
          onChange={setPriority}
        />
      </div>
      {store.saveError === 'invalid' && (
        <p className="notification is-danger is-light" role="alert">
          {t('taskDetail.invalid')}
        </p>
      )}
      {store.saveError === 'failed' && (
        <p className="notification is-danger is-light" role="alert">
          {t('taskDetail.saveFailed')}
        </p>
      )}
      <div className="buttons is-right">
        <button type="button" className="button" onClick={discard}>
          {t('taskDetail.discard')}
        </button>
        <button type="submit" className={store.saving ? 'button is-primary is-loading' : 'button is-primary'}>
          {t('taskDetail.save')}
        </button>
      </div>
    </form>
  );
});
