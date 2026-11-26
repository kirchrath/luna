import { Archive, ArchiveRestore, ChevronLeft, Sparkles } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect, useState, type SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';

import type { NoteDto } from '../api';
import { ID_PARAMETER, PATHS } from '../navigation';
import { useStores } from '../stores';
import { RelativeTime } from '../ui';
import { formatDateTime } from './taskDates';

export const MAX_NOTE_TITLE_LENGTH = 500;
export const MAX_SUMMARY_LENGTH = 10_000;
const ICON_SIZE = 16;
const IDS = {
  title: 'note-title',
  titleHelp: 'note-title-help',
  summary: 'note-summary',
  summaryHelp: 'note-summary-help',
} as const;

export const NoteDetailPage = observer(function NoteDetailPage() {
  const { t } = useTranslation();
  const { noteStore: store } = useStores();
  // The route always contains the parameter.
  const id = String(useParams()[ID_PARAMETER]);

  useEffect(() => {
    void store.load(id);
  }, [store, id]);

  const note = store.id === id ? store.note : null;

  return (
    <section className="page">
      <Link className="element-back has-text-grey" to={PATHS.notes}>
        <ChevronLeft size={ICON_SIZE} aria-hidden="true" />
        <span>{t('noteDetail.back')}</span>
      </Link>
      {note === null ? (
        <NoteLoadMessage />
      ) : (
        // A new version (saved, archived or reloaded) resets the form to the stored state.
        <NoteDetail key={note.audit.version} note={note} />
      )}
    </section>
  );
});

const NoteLoadMessage = observer(function NoteLoadMessage() {
  const { t } = useTranslation();
  const { noteStore: store } = useStores();

  if (store.loadState === 'notFound') {
    return (
      <div className="box">
        <h1 className="title is-4">{t('noteDetail.notFound')}</h1>
        <p className="has-text-grey">{t('noteDetail.notFoundText')}</p>
      </div>
    );
  }
  if (store.loadState === 'failed') {
    return (
      <div className="notification is-danger is-light" role="alert">
        <p>{t('noteDetail.loadError')}</p>
        <button type="button" className="button is-small" onClick={() => void store.reload()}>
          {t('noteDetail.retry')}
        </button>
      </div>
    );
  }
  return <progress className="progress is-small is-primary" aria-label={t('noteDetail.formLabel')} />;
});

const NoteDetail = observer(function NoteDetail({ note }: { note: NoteDto }) {
  const { t, i18n } = useTranslation();
  const { noteStore: store } = useStores();
  const { data, audit } = note;
  const archived = data.status === 'ARCHIVED';
  const StatusIcon = archived ? ArchiveRestore : Archive;

  return (
    <>
      <div className="element-heading">
        <h1 className="title">{data.title}</h1>
        <button
          type="button"
          className={
            store.statusChanging ? 'button is-primary is-outlined is-loading' : 'button is-primary is-outlined'
          }
          onClick={() => void (archived ? store.restore() : store.archive())}
        >
          <StatusIcon size={ICON_SIZE} aria-hidden="true" />
          <span>{t(archived ? 'noteDetail.restore' : 'noteDetail.archive')}</span>
        </button>
      </div>
      <p className="element-meta-line is-size-7 has-text-grey">
        <span className="tag is-rounded">{t(`notes.status.${data.status}`)}</span>
        <span>
          {t('taskDetail.version', { version: audit.version })} · {t('taskDetail.changed')}{' '}
          <RelativeTime dateTime={audit.lastModifiedDate} locale={i18n.language} />
        </span>
      </p>
      {store.statusError && (
        <p className="notification is-danger is-light" role="alert">
          {t('noteDetail.statusFailed')}
        </p>
      )}
      {store.saveError === 'conflict' && (
        <div className="notification is-link is-light element-conflict" role="alert">
          <div>
            <p className="has-text-weight-semibold">{t('noteDetail.conflict')}</p>
            <p className="is-size-7">{t('noteDetail.conflictText')}</p>
          </div>
          <button type="button" className="button is-small is-link is-outlined" onClick={() => void store.reload()}>
            {t('noteDetail.reload')}
          </button>
        </div>
      )}
      <NoteForm note={note} />
      <section className="box" aria-labelledby="note-origin-title">
        <h2 id="note-origin-title" className="title is-6">
          {t('noteDetail.origin')}
        </h2>
        <p className="element-origin">
          <Sparkles size={ICON_SIZE} aria-hidden="true" />
          <span>{t(`noteDetail.source.${data.source}`)}</span>
        </p>
        <p className="is-size-7 has-text-grey">
          {t(data.edited ? 'noteDetail.editedHint' : 'noteDetail.uneditedHint')}
        </p>
        <p className="is-size-7 has-text-grey">
          {t('noteDetail.created', { date: formatDateTime(audit.createdDate, i18n.language) })} ·{' '}
          {t('noteDetail.modified', { date: formatDateTime(audit.lastModifiedDate, i18n.language) })}
          {data.archivedDate !== undefined &&
            ` · ${t('noteDetail.archived', { date: formatDateTime(data.archivedDate, i18n.language) })}`}
        </p>
      </section>
    </>
  );
});

const NoteForm = observer(function NoteForm({ note }: { note: NoteDto }) {
  const { t } = useTranslation();
  const { noteStore: store } = useStores();
  const [title, setTitle] = useState(note.data.title);
  const [summary, setSummary] = useState(note.data.summary);
  const [titleMissing, setTitleMissing] = useState(false);
  const [summaryMissing, setSummaryMissing] = useState(false);

  const discard = () => {
    setTitle(note.data.title);
    setSummary(note.data.summary);
    setTitleMissing(false);
    setSummaryMissing(false);
  };

  const save = async (event: SyntheticEvent) => {
    event.preventDefault();
    const trimmedTitle = title.trim();
    const trimmedSummary = summary.trim();
    setTitleMissing(trimmedTitle === '');
    setSummaryMissing(trimmedSummary === '');
    if (trimmedTitle === '' || trimmedSummary === '') {
      return;
    }
    await store.save({ title: trimmedTitle, summary: trimmedSummary });
  };

  return (
    <form
      className="box element-form"
      aria-label={t('noteDetail.formLabel')}
      noValidate
      onSubmit={(event) => void save(event)}
    >
      <div className="field">
        <label className="label" htmlFor={IDS.title}>
          {t('noteDetail.titleLabel')}
        </label>
        <div className="control">
          <input
            id={IDS.title}
            className={titleMissing ? 'input is-danger' : 'input'}
            value={title}
            maxLength={MAX_NOTE_TITLE_LENGTH}
            aria-invalid={titleMissing}
            aria-describedby={IDS.titleHelp}
            onChange={(event) => {
              setTitle(event.target.value);
              setTitleMissing(false);
            }}
          />
        </div>
        {titleMissing && (
          <p id={IDS.titleHelp} className="help is-danger">
            {t('noteDetail.titleMissing')}
          </p>
        )}
      </div>
      <div className="field">
        <label className="label" htmlFor={IDS.summary}>
          {t('noteDetail.summaryLabel')}
        </label>
        <div className="control">
          <textarea
            id={IDS.summary}
            className={summaryMissing ? 'textarea is-danger' : 'textarea'}
            rows={6}
            value={summary}
            maxLength={MAX_SUMMARY_LENGTH}
            aria-invalid={summaryMissing}
            aria-describedby={IDS.summaryHelp}
            onChange={(event) => {
              setSummary(event.target.value);
              setSummaryMissing(false);
            }}
          />
        </div>
        {summaryMissing && (
          <p id={IDS.summaryHelp} className="help is-danger">
            {t('noteDetail.summaryMissing')}
          </p>
        )}
      </div>
      {store.saveError === 'invalid' && (
        <p className="notification is-danger is-light" role="alert">
          {t('noteDetail.invalid')}
        </p>
      )}
      {store.saveError === 'failed' && (
        <p className="notification is-danger is-light" role="alert">
          {t('noteDetail.saveFailed')}
        </p>
      )}
      <div className="buttons is-right">
        <button type="button" className="button" onClick={discard}>
          {t('noteDetail.discard')}
        </button>
        <button type="submit" className={store.saving ? 'button is-primary is-loading' : 'button is-primary'}>
          {t('noteDetail.save')}
        </button>
      </div>
    </form>
  );
});
