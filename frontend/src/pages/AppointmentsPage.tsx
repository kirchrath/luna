import { CalendarDays, ChevronLeft, ChevronRight, List, MapPin } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';

import type { AppointmentListItemDto, AppointmentPeriodDto } from '../api';
import { appointmentPath } from '../navigation';
import { readPreference, STORAGE_KEYS, writePreference } from '../services';
import { useStores } from '../stores';
import { SegmentedControl } from '../ui';
import {
  addDays,
  daysOf,
  formatDay,
  formatMonth,
  formatTime,
  groupByDay,
  isSameDay,
  monthWeeks,
  startOfDay,
  weekdayNames,
  type AppointmentDay,
} from './appointmentDates';
import { LifeAreaIcon } from './LifeArea';

type View = 'list' | 'month';
const MONTH: View = 'month';
const LIST: View = 'list';
const ICON_SIZE = 16;
const CHEVRON_SIZE = 18;
const PERIODS: readonly AppointmentPeriodDto[] = ['UPCOMING', 'PAST', 'ALL'];

export function AppointmentsPage() {
  const { t } = useTranslation();
  const [view, setView] = useState<View>(() => (readPreference(STORAGE_KEYS.appointmentView) === MONTH ? MONTH : LIST));
  const changeView = (next: View) => {
    writePreference(STORAGE_KEYS.appointmentView, next);
    setView(next);
  };

  return (
    <section className="page">
      <h1 className="title">{t('appointments.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('appointments.lead')}</p>
      {view === LIST ? <AppointmentAgenda onViewChange={changeView} /> : <AppointmentMonth onViewChange={changeView} />}
    </section>
  );
}

function ViewToggle({ view, onChange }: { view: View; onChange: (view: View) => void }) {
  const { t } = useTranslation();
  return (
    <div className="buttons has-addons view-toggle" role="group" aria-label={t('appointments.viewLabel')}>
      {([LIST, MONTH] as const).map((option) => {
        const Icon = option === LIST ? List : CalendarDays;
        return (
          <button
            key={option}
            type="button"
            className={option === view ? 'button is-small is-primary' : 'button is-small'}
            aria-pressed={option === view}
            aria-label={t(`appointments.view.${option}`)}
            title={t(`appointments.view.${option}`)}
            onClick={() => {
              onChange(option);
            }}
          >
            <Icon size={ICON_SIZE} aria-hidden="true" />
          </button>
        );
      })}
    </div>
  );
}

/** "Today, Thu 26 November", "Tomorrow, …" or just the day. */
function DayHeading({ day }: { day: Date }) {
  const { t, i18n } = useTranslation();
  const today = startOfDay(new Date());
  const formatted = formatDay(day, i18n.language);
  if (isSameDay(day, today)) {
    return <>{t('appointments.today', { day: formatted })}</>;
  }
  if (isSameDay(day, addDays(today, 1))) {
    return <>{t('appointments.tomorrow', { day: formatted })}</>;
  }
  return <>{formatted}</>;
}

function TimeLabel({ entry }: { entry: AppointmentDay<AppointmentListItemDto> }) {
  const { t, i18n } = useTranslation();
  const { appointment, dayNumber, dayCount } = entry;
  const start = formatTime(appointment.start, i18n.language);
  const end = appointment.end === undefined ? undefined : formatTime(appointment.end, i18n.language);
  if (appointment.allDay || (dayNumber > 1 && dayNumber < dayCount)) {
    return <>{t('appointments.allDay')}</>;
  }
  if (dayCount > 1) {
    return <>{dayNumber === 1 ? t('appointments.from', { time: start }) : t('appointments.until', { time: end })}</>;
  }
  return <>{end === undefined ? start : `${start}–${end}`}</>;
}

function AppointmentEntry({ entry }: { entry: AppointmentDay<AppointmentListItemDto> }) {
  const { t } = useTranslation();
  const { appointment, dayNumber, dayCount } = entry;
  const cancelled = appointment.status === 'CANCELLED';
  return (
    <li className={cancelled ? 'appointment is-cancelled' : 'appointment'}>
      <Link className="appointment-link" to={appointmentPath(appointment.id)}>
        <span className="appointment-time">
          <TimeLabel entry={entry} />
        </span>
        <span className="element-body">
          <span className="appointment-title-line">
            <span className="element-title">{appointment.title}</span>
            <LifeAreaIcon area={appointment.area} />
            {cancelled && <span className="tag is-rounded">{t('appointments.cancelled')}</span>}
          </span>
          {(appointment.location !== undefined || dayCount > 1) && (
            <span className="element-meta has-text-grey">
              {appointment.location !== undefined && (
                <>
                  <MapPin size={ICON_SIZE - 3} aria-hidden="true" />
                  <span>{appointment.location}</span>
                </>
              )}
              {dayCount > 1 && <span>{t('appointments.dayOf', { day: dayNumber, count: dayCount })}</span>}
            </span>
          )}
        </span>
        <ChevronRight className="has-text-grey" size={CHEVRON_SIZE} aria-hidden="true" />
      </Link>
    </li>
  );
}

function DayGroups({ groups }: { groups: ReturnType<typeof groupByDay<AppointmentListItemDto>> }) {
  return (
    <>
      {groups.map(({ day, entries }) => (
        <section key={day.getTime()} className="appointment-day">
          <h2 className="appointment-day-title">
            <DayHeading day={day} />
          </h2>
          <ul>
            {entries.map((entry) => (
              <AppointmentEntry key={entry.appointment.id} entry={entry} />
            ))}
          </ul>
        </section>
      ))}
    </>
  );
}

const AppointmentAgenda = observer(function AppointmentAgenda({
  onViewChange,
}: {
  onViewChange: (view: View) => void;
}) {
  const { t } = useTranslation();
  const { appointmentListStore: store } = useStores();

  useEffect(() => {
    void store.loadInitial();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  // Upcoming multi-day appointments that already started are only shown from today on.
  const from = store.filter === 'UPCOMING' ? new Date() : undefined;
  const groups = groupByDay(store.items, from);
  const ordered = store.filter === 'PAST' ? [...groups].reverse() : groups;

  return (
    <>
      <div className="element-list-toolbar">
        <SegmentedControl
          label={t('appointments.periodLabel')}
          options={PERIODS.map((period) => ({ value: period, label: t(`appointments.period.${period}`) }))}
          value={store.filter}
          onChange={(period) => void store.changeFilter(period)}
        />
        <ViewToggle view={LIST} onChange={onViewChange} />
      </div>
      <section className="box element-list" aria-label={t('appointments.title')}>
        <ConnectionHint disconnected={store.connection === 'disconnected'} />
        {store.loadError && <LoadError onRetry={() => void store.refresh()} />}
        {store.items.length === 0 && !store.loading && !store.loadError && (
          <p className="has-text-grey">{t(`appointments.empty.${store.filter}`)}</p>
        )}
        <DayGroups groups={ordered} />
        {store.hasMore && (
          <button
            type="button"
            className={store.loading ? 'button is-fullwidth is-loading' : 'button is-fullwidth'}
            onClick={() => void store.loadMore()}
          >
            {t('appointments.loadMore')}
          </button>
        )}
      </section>
    </>
  );
});

const AppointmentMonth = observer(function AppointmentMonth({ onViewChange }: { onViewChange: (view: View) => void }) {
  const { t, i18n } = useTranslation();
  const { appointmentMonthStore: store } = useStores();

  useEffect(() => {
    void store.load();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  const today = startOfDay(new Date());
  const byDay = groupByDay(store.items);
  const entriesOf = (day: Date) => byDay.find((group) => isSameDay(group.day, day))?.entries ?? [];
  const selected = store.selectedDay;

  return (
    <>
      <div className="element-list-toolbar">
        <div className="month-navigation">
          <button
            type="button"
            className="button is-small is-white"
            aria-label={t('appointments.previousMonth')}
            onClick={() => void store.previousMonth()}
          >
            <ChevronLeft size={CHEVRON_SIZE} aria-hidden="true" />
          </button>
          <h2 className="month-title">{formatMonth(store.month, i18n.language)}</h2>
          <button
            type="button"
            className="button is-small is-white"
            aria-label={t('appointments.nextMonth')}
            onClick={() => void store.nextMonth()}
          >
            <ChevronRight size={CHEVRON_SIZE} aria-hidden="true" />
          </button>
          <button type="button" className="button is-small" onClick={() => void store.showToday()}>
            {t('appointments.todayButton')}
          </button>
        </div>
        <ViewToggle view={MONTH} onChange={onViewChange} />
      </div>
      <ConnectionHint disconnected={store.connection === 'disconnected'} />
      {store.loadError && <LoadError onRetry={() => void store.load()} />}
      <div className="box month" role="grid" aria-label={formatMonth(store.month, i18n.language)}>
        <div className="month-row month-weekdays" role="row">
          {weekdayNames(i18n.language).map((name) => (
            <span key={name} className="month-weekday has-text-grey" role="columnheader">
              {name}
            </span>
          ))}
        </div>
        {monthWeeks(store.month).map((week) => (
          <div key={week.find((day) => day !== null)?.getTime()} className="month-row" role="row">
            {week.map((day, index) =>
              day === null ? (
                <span key={index} className="month-day is-empty" role="gridcell" />
              ) : (
                <div
                  key={day.getTime()}
                  className={selected !== null && isSameDay(day, selected) ? 'month-day is-selected' : 'month-day'}
                  role="gridcell"
                >
                  <button
                    type="button"
                    className={isSameDay(day, today) ? 'month-day-number is-today' : 'month-day-number'}
                    aria-label={formatDay(day, i18n.language)}
                    aria-pressed={selected !== null && isSameDay(day, selected)}
                    onClick={() => {
                      store.selectDay(day);
                    }}
                  >
                    {day.getDate()}
                  </button>
                  {entriesOf(day).map(({ appointment }) => (
                    <Link
                      key={appointment.id}
                      className={appointment.status === 'CANCELLED' ? 'month-event is-cancelled' : 'month-event'}
                      to={appointmentPath(appointment.id)}
                      title={appointment.title}
                    >
                      <span className="month-event-title">
                        {!appointment.allDay && daysOf(appointment).length === 1
                          ? `${formatTime(appointment.start, i18n.language)} ${appointment.title}`
                          : appointment.title}
                      </span>
                    </Link>
                  ))}
                </div>
              ),
            )}
          </div>
        ))}
      </div>
      {selected !== null && (
        <section className="month-selection" aria-label={formatDay(selected, i18n.language)}>
          <h2 className="appointment-day-title">
            <DayHeading day={selected} />
          </h2>
          <div className="box element-list">
            {entriesOf(selected).length === 0 ? (
              <p className="has-text-grey">{t('appointments.noAppointmentsOnDay')}</p>
            ) : (
              <ul>
                {entriesOf(selected).map((entry) => (
                  <AppointmentEntry key={entry.appointment.id} entry={entry} />
                ))}
              </ul>
            )}
          </div>
        </section>
      )}
    </>
  );
});

function ConnectionHint({ disconnected }: { disconnected: boolean }) {
  const { t } = useTranslation();
  return disconnected ? (
    <p className="notification is-warning is-light is-size-7" role="status">
      {t('appointments.liveInterrupted')}
    </p>
  ) : null;
}

function LoadError({ onRetry }: { onRetry: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="notification is-danger is-light" role="alert">
      <p>{t('appointments.loadError')}</p>
      <button type="button" className="button is-small" onClick={onRetry}>
        {t('appointments.retry')}
      </button>
    </div>
  );
}
