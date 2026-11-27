import { CalendarCheck, CalendarX, ChevronLeft, Sparkles } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect, useState, type SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';

import type { AppointmentDto } from '../api';
import { ID_PARAMETER, PATHS } from '../navigation';
import { useStores } from '../stores';
import { formatDay, formatTime, fromDateInput, toDateInput } from './appointmentDates';
import { formatDateTime } from './taskDates';
import { LifeAreaDetail } from './LifeArea';

export const MAX_APPOINTMENT_TITLE_LENGTH = 500;
export const MAX_LOCATION_LENGTH = 500;
export const MAX_APPOINTMENT_DESCRIPTION_LENGTH = 10_000;
const ICON_SIZE = 16;
const IDS = {
  title: 'appointment-title',
  titleHelp: 'appointment-title-help',
  allDay: 'appointment-all-day',
  startDate: 'appointment-start-date',
  startTime: 'appointment-start-time',
  endDate: 'appointment-end-date',
  endTime: 'appointment-end-time',
  timesHelp: 'appointment-times-help',
  location: 'appointment-location',
  description: 'appointment-description',
} as const;

export const AppointmentDetailPage = observer(function AppointmentDetailPage() {
  const { t } = useTranslation();
  const { appointmentStore: store } = useStores();
  // The route always contains the parameter.
  const id = String(useParams()[ID_PARAMETER]);

  useEffect(() => {
    void store.load(id);
  }, [store, id]);

  const appointment = store.id === id ? store.appointment : null;

  return (
    <section className="page">
      <Link className="element-back has-text-grey" to={PATHS.calendar}>
        <ChevronLeft size={ICON_SIZE} aria-hidden="true" />
        <span>{t('appointmentDetail.back')}</span>
      </Link>
      {appointment === null ? (
        <AppointmentLoadMessage />
      ) : (
        // A new version (saved, cancelled or reloaded) resets the form to the stored state.
        <AppointmentDetail key={appointment.audit.version} appointment={appointment} />
      )}
    </section>
  );
});

const AppointmentLoadMessage = observer(function AppointmentLoadMessage() {
  const { t } = useTranslation();
  const { appointmentStore: store } = useStores();

  if (store.loadState === 'notFound') {
    return (
      <div className="box">
        <h1 className="title is-4">{t('appointmentDetail.notFound')}</h1>
        <p className="has-text-grey">{t('appointmentDetail.notFoundText')}</p>
      </div>
    );
  }
  if (store.loadState === 'failed') {
    return (
      <div className="notification is-danger is-light" role="alert">
        <p>{t('appointmentDetail.loadError')}</p>
        <button type="button" className="button is-small" onClick={() => void store.reload()}>
          {t('appointmentDetail.retry')}
        </button>
      </div>
    );
  }
  return <progress className="progress is-small is-primary" aria-label={t('appointmentDetail.formLabel')} />;
});

/** E.g. "Tue 1 December, 10:00–11:00" or "Tue 1 December – Thu 3 December, all day". */
function when(appointment: AppointmentDto, locale: string, allDayLabel: string): string {
  const { start, end, allDay } = appointment.data;
  const startDay = formatDay(new Date(start), locale);
  const endDay = end === undefined ? startDay : formatDay(new Date(end), locale);
  const days = endDay === startDay ? startDay : `${startDay} – ${endDay}`;
  if (allDay) {
    return `${days}, ${allDayLabel}`;
  }
  const startTime = formatTime(start, locale);
  if (end === undefined) {
    return `${days}, ${startTime}`;
  }
  const endTime = formatTime(end, locale);
  return endDay === startDay ? `${days}, ${startTime}–${endTime}` : `${startDay}, ${startTime} – ${endDay}, ${endTime}`;
}

const AppointmentDetail = observer(function AppointmentDetail({ appointment }: { appointment: AppointmentDto }) {
  const { t, i18n } = useTranslation();
  const { appointmentStore: store } = useStores();
  const { data, audit } = appointment;
  const cancelled = data.status === 'CANCELLED';
  const StatusIcon = cancelled ? CalendarCheck : CalendarX;

  return (
    <>
      <div className="element-heading">
        <h1 className={cancelled ? 'title is-cancelled' : 'title'}>{data.title}</h1>
        <button
          type="button"
          className={
            store.statusChanging ? 'button is-primary is-outlined is-loading' : 'button is-primary is-outlined'
          }
          onClick={() => void (cancelled ? store.restore() : store.cancel())}
        >
          <StatusIcon size={ICON_SIZE} aria-hidden="true" />
          <span>{t(cancelled ? 'appointmentDetail.restore' : 'appointmentDetail.cancel')}</span>
        </button>
      </div>
      <p className="element-meta-line is-size-7 has-text-grey">
        <span className="tag is-rounded">{t(`appointments.status.${data.status}`)}</span>
        <span>
          {when(appointment, i18n.language, t('appointments.allDay'))} ·{' '}
          {t('taskDetail.version', { version: audit.version })}
        </span>
      </p>
      {store.statusError && (
        <p className="notification is-danger is-light" role="alert">
          {t('appointmentDetail.statusFailed')}
        </p>
      )}
      {store.saveError === 'conflict' && (
        <div className="notification is-link is-light element-conflict" role="alert">
          <div>
            <p className="has-text-weight-semibold">{t('appointmentDetail.conflict')}</p>
            <p className="is-size-7">{t('appointmentDetail.conflictText')}</p>
          </div>
          <button type="button" className="button is-small is-link is-outlined" onClick={() => void store.reload()}>
            {t('appointmentDetail.reload')}
          </button>
        </div>
      )}
      <AppointmentForm appointment={appointment} />
      <section className="box" aria-labelledby="appointment-origin-title">
        <h2 id="appointment-origin-title" className="title is-6">
          {t('appointmentDetail.origin')}
        </h2>
        <p className="element-origin">
          <Sparkles size={ICON_SIZE} aria-hidden="true" />
          <span>{t(`appointmentDetail.source.${data.source}`)}</span>
        </p>
        <LifeAreaDetail area={data.area} />
        <p className="is-size-7 has-text-grey">
          {t(data.edited ? 'appointmentDetail.editedHint' : 'appointmentDetail.uneditedHint')}
        </p>
        <p className="is-size-7 has-text-grey">
          {t('appointmentDetail.created', { date: formatDateTime(audit.createdDate, i18n.language) })} ·{' '}
          {t('appointmentDetail.modified', { date: formatDateTime(audit.lastModifiedDate, i18n.language) })}
          {data.cancelledDate !== undefined &&
            ` · ${t('appointmentDetail.cancelledOn', { date: formatDateTime(data.cancelledDate, i18n.language) })}`}
        </p>
      </section>
    </>
  );
});

const AppointmentForm = observer(function AppointmentForm({ appointment }: { appointment: AppointmentDto }) {
  const { t } = useTranslation();
  const { appointmentStore: store } = useStores();
  const { data } = appointment;
  const initial = {
    title: data.title,
    allDay: data.allDay,
    start: toDateInput(data.start),
    end: toDateInput(data.end),
    location: data.location ?? '',
    description: data.description ?? '',
  };
  const [title, setTitle] = useState(initial.title);
  const [allDay, setAllDay] = useState(initial.allDay);
  const [startDate, setStartDate] = useState(initial.start.date);
  const [startTime, setStartTime] = useState(initial.start.time);
  const [endDate, setEndDate] = useState(initial.end.date);
  const [endTime, setEndTime] = useState(initial.end.time);
  const [location, setLocation] = useState(initial.location);
  const [description, setDescription] = useState(initial.description);
  const [titleMissing, setTitleMissing] = useState(false);
  const [timesInvalid, setTimesInvalid] = useState(false);

  const discard = () => {
    setTitle(initial.title);
    setAllDay(initial.allDay);
    setStartDate(initial.start.date);
    setStartTime(initial.start.time);
    setEndDate(initial.end.date);
    setEndTime(initial.end.time);
    setLocation(initial.location);
    setDescription(initial.description);
    setTitleMissing(false);
    setTimesInvalid(false);
  };

  const save = async (event: SyntheticEvent) => {
    event.preventDefault();
    const trimmedTitle = title.trim();
    const start = fromDateInput(startDate, startTime, allDay);
    const end = fromDateInput(endDate, endTime, allDay);
    const invalidTimes = start === undefined || (end !== undefined && end < start);
    setTitleMissing(trimmedTitle === '');
    setTimesInvalid(invalidTimes);
    if (trimmedTitle === '' || invalidTimes) {
      return;
    }
    const trimmedLocation = location.trim();
    const trimmedDescription = description.trim();
    await store.save({
      title: trimmedTitle,
      start,
      allDay,
      ...(end === undefined ? {} : { end }),
      ...(trimmedLocation === '' ? {} : { location: trimmedLocation }),
      ...(trimmedDescription === '' ? {} : { description: trimmedDescription }),
    });
  };

  return (
    <form
      className="box element-form"
      aria-label={t('appointmentDetail.formLabel')}
      noValidate
      onSubmit={(event) => void save(event)}
    >
      <div className="field">
        <label className="label" htmlFor={IDS.title}>
          {t('appointmentDetail.titleLabel')}
        </label>
        <div className="control">
          <input
            id={IDS.title}
            className={titleMissing ? 'input is-danger' : 'input'}
            value={title}
            maxLength={MAX_APPOINTMENT_TITLE_LENGTH}
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
            {t('appointmentDetail.titleMissing')}
          </p>
        )}
      </div>
      <div className="field">
        <label className="checkbox" htmlFor={IDS.allDay}>
          <input
            id={IDS.allDay}
            type="checkbox"
            checked={allDay}
            onChange={(event) => {
              setAllDay(event.target.checked);
            }}
          />{' '}
          {t('appointmentDetail.allDayLabel')}
        </label>
      </div>
      <div className="columns is-mobile is-multiline appointment-times">
        <div className="column is-full-mobile">
          <label className="label" htmlFor={IDS.startDate}>
            {t('appointmentDetail.startLabel')}
          </label>
          <div className="appointment-time-inputs">
            <input
              id={IDS.startDate}
              className={timesInvalid ? 'input is-danger' : 'input'}
              type="date"
              value={startDate}
              aria-describedby={IDS.timesHelp}
              onChange={(event) => {
                setStartDate(event.target.value);
                setTimesInvalid(false);
              }}
            />
            {!allDay && (
              <input
                id={IDS.startTime}
                className="input"
                type="time"
                value={startTime}
                aria-label={t('appointmentDetail.startTimeLabel')}
                onChange={(event) => {
                  setStartTime(event.target.value);
                  setTimesInvalid(false);
                }}
              />
            )}
          </div>
        </div>
        <div className="column is-full-mobile">
          <label className="label" htmlFor={IDS.endDate}>
            {t(allDay ? 'appointmentDetail.lastDayLabel' : 'appointmentDetail.endLabel')}
          </label>
          <div className="appointment-time-inputs">
            <input
              id={IDS.endDate}
              className={timesInvalid ? 'input is-danger' : 'input'}
              type="date"
              value={endDate}
              onChange={(event) => {
                setEndDate(event.target.value);
                setTimesInvalid(false);
              }}
            />
            {!allDay && (
              <input
                id={IDS.endTime}
                className="input"
                type="time"
                value={endTime}
                aria-label={t('appointmentDetail.endTimeLabel')}
                onChange={(event) => {
                  setEndTime(event.target.value);
                  setTimesInvalid(false);
                }}
              />
            )}
          </div>
        </div>
      </div>
      {timesInvalid && (
        <p id={IDS.timesHelp} className="help is-danger appointment-times-help">
          {t('appointmentDetail.timesInvalid')}
        </p>
      )}
      <div className="field">
        <label className="label" htmlFor={IDS.location}>
          {t('appointmentDetail.locationLabel')}
        </label>
        <div className="control">
          <input
            id={IDS.location}
            className="input"
            value={location}
            maxLength={MAX_LOCATION_LENGTH}
            onChange={(event) => {
              setLocation(event.target.value);
            }}
          />
        </div>
      </div>
      <div className="field">
        <label className="label" htmlFor={IDS.description}>
          {t('appointmentDetail.descriptionLabel')}
        </label>
        <div className="control">
          <textarea
            id={IDS.description}
            className="textarea"
            rows={3}
            value={description}
            maxLength={MAX_APPOINTMENT_DESCRIPTION_LENGTH}
            onChange={(event) => {
              setDescription(event.target.value);
            }}
          />
        </div>
      </div>
      {store.saveError === 'invalid' && (
        <p className="notification is-danger is-light" role="alert">
          {t('appointmentDetail.invalid')}
        </p>
      )}
      {store.saveError === 'failed' && (
        <p className="notification is-danger is-light" role="alert">
          {t('appointmentDetail.saveFailed')}
        </p>
      )}
      <div className="buttons is-right">
        <button type="button" className="button" onClick={discard}>
          {t('appointmentDetail.discard')}
        </button>
        <button type="submit" className={store.saving ? 'button is-primary is-loading' : 'button is-primary'}>
          {t('appointmentDetail.save')}
        </button>
      </div>
    </form>
  );
});
