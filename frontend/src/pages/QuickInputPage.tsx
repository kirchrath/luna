import { useTranslation } from 'react-i18next';

export function QuickInputPage() {
  const { t } = useTranslation();
  return (
    <section className="page">
      <h1 className="title">{t('quickInput.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('quickInput.lead')}</p>
    </section>
  );
}
