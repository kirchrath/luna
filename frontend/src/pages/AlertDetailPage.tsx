import { Check, ChevronLeft, Eye, RotateCcw, Server, Sparkles } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';

import type { AlertDto } from '../api';
import { ID_PARAMETER, PATHS } from '../navigation';
import { useStores } from '../stores';
import { RelativeTime } from '../ui';
import { SeverityTag } from './SeverityTag';
import { formatDateTime } from './taskDates';
import { LifeAreaDetail } from './LifeArea';

const ICON_SIZE = 16;

export const AlertDetailPage = observer(function AlertDetailPage() {
  const { t } = useTranslation();
  const { alertStore: store } = useStores();
  // The route always contains the parameter.
  const id = String(useParams()[ID_PARAMETER]);

  useEffect(() => {
    void store.load(id);
  }, [store, id]);

  const alert = store.id === id ? store.alert : null;

  return (
    <section className="page">
      <Link className="element-back has-text-grey" to={PATHS.alerts}>
        <ChevronLeft size={ICON_SIZE} aria-hidden="true" />
        <span>{t('alertDetail.back')}</span>
      </Link>
      {alert === null ? <AlertLoadMessage /> : <AlertDetail alert={alert} />}
    </section>
  );
});

const AlertLoadMessage = observer(function AlertLoadMessage() {
  const { t } = useTranslation();
  const { alertStore: store } = useStores();

  if (store.loadState === 'notFound') {
    return (
      <div className="box">
        <h1 className="title is-4">{t('alertDetail.notFound')}</h1>
        <p className="has-text-grey">{t('alertDetail.notFoundText')}</p>
      </div>
    );
  }
  if (store.loadState === 'failed') {
    return (
      <div className="notification is-danger is-light" role="alert">
        <p>{t('alertDetail.loadError')}</p>
        <button type="button" className="button is-small" onClick={() => void store.reload()}>
          {t('alertDetail.retry')}
        </button>
      </div>
    );
  }
  return <progress className="progress is-small is-primary" aria-label={t('alertDetail.loading')} />;
});

const AlertDetail = observer(function AlertDetail({ alert }: { alert: AlertDto }) {
  const { t, i18n } = useTranslation();
  const { alertStore: store } = useStores();
  const { data, audit } = alert;
  const buttonClass = (primary: boolean) =>
    `button ${primary ? 'is-primary' : 'is-primary is-outlined'}${store.statusChanging ? ' is-loading' : ''}`;
  const dates = [
    t('alertDetail.created', { date: formatDateTime(audit.createdDate, i18n.language) }),
    ...(data.acknowledgedDate === undefined
      ? []
      : [t('alertDetail.acknowledged', { date: formatDateTime(data.acknowledgedDate, i18n.language) })]),
    ...(data.resolvedDate === undefined
      ? []
      : [t('alertDetail.resolved', { date: formatDateTime(data.resolvedDate, i18n.language) })]),
  ];

  return (
    <>
      <h1 className="title">{data.title}</h1>
      <p className="element-meta-line is-size-7 has-text-grey">
        <SeverityTag severity={data.severity} />
        <span className="tag is-rounded">{t(`alerts.status.${data.status}`)}</span>
        <span>
          {t('alertDetail.received')} <RelativeTime dateTime={audit.createdDate} locale={i18n.language} />
        </span>
      </p>
      <div className="buttons alert-actions">
        {data.status === 'OPEN' && (
          <button type="button" className={buttonClass(true)} onClick={() => void store.acknowledge()}>
            <Eye size={ICON_SIZE} aria-hidden="true" />
            <span>{t('alertDetail.acknowledge')}</span>
          </button>
        )}
        {data.status !== 'RESOLVED' && (
          <button type="button" className={buttonClass(false)} onClick={() => void store.resolve()}>
            <Check size={ICON_SIZE} aria-hidden="true" />
            <span>{t('alertDetail.resolve')}</span>
          </button>
        )}
        {data.status !== 'OPEN' && (
          <button type="button" className={buttonClass(false)} onClick={() => void store.reopen()}>
            <RotateCcw size={ICON_SIZE} aria-hidden="true" />
            <span>{t('alertDetail.reopen')}</span>
          </button>
        )}
      </div>
      {store.statusError && (
        <p className="notification is-danger is-light" role="alert">
          {t('alertDetail.statusFailed')}
        </p>
      )}
      <section className="box" aria-label={t('alertDetail.message')}>
        <p className="label">{t('alertDetail.message')}</p>
        <p className="alert-message">{data.message}</p>
        {data.affectedSystem !== undefined && (
          <>
            <p className="label mt-4">{t('alertDetail.affectedSystem')}</p>
            <p className="element-origin">
              <Server size={ICON_SIZE} aria-hidden="true" />
              <span>{data.affectedSystem}</span>
            </p>
          </>
        )}
      </section>
      <section className="box" aria-labelledby="alert-origin-title">
        <h2 id="alert-origin-title" className="title is-6">
          {t('alertDetail.origin')}
        </h2>
        <p className="element-origin">
          <Sparkles size={ICON_SIZE} aria-hidden="true" />
          <span>{t(`alertDetail.source.${data.source}`)}</span>
        </p>
        <LifeAreaDetail area={data.area} />
        <p className="is-size-7 has-text-grey">{dates.join(' · ')}</p>
      </section>
    </>
  );
});
