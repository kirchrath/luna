import { Send } from 'lucide-react';
import { observer } from 'mobx-react-lite';
import { useEffect, useRef, useState, type KeyboardEvent, type SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { useStores } from '../stores';
import { ChipGroup } from '../ui';
import { AUTOMATIC, AUTOMATIC_ICON, ELEMENT_TYPE_ICONS, ELEMENT_TYPE_ORDER, type TypeChoice } from './elementTypes';
import { InboundEventList } from './InboundEventList';

export const MAX_TEXT_LENGTH = 10_000;
const CHIP_ICON_SIZE = 16;
const ENTER = 'Enter';
const TEXT_ID = 'quick-input-text';
const TEXT_HELP_ID = 'quick-input-text-help';

export const QuickInputPage = observer(function QuickInputPage() {
  const { t, i18n } = useTranslation();
  const { inboundEventStore: store } = useStores();
  const [text, setText] = useState('');
  const [typeChoice, setTypeChoice] = useState<TypeChoice>(AUTOMATIC);
  const [textMissing, setTextMissing] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    void store.loadInitial();
    store.connect();
    return () => {
      void store.disconnect();
    };
  }, [store]);

  const submit = async (event: SyntheticEvent) => {
    event.preventDefault();
    const trimmed = text.trim();
    if (trimmed === '') {
      setTextMissing(true);
      return;
    }
    const accepted = await store.submit(trimmed, typeChoice === AUTOMATIC ? undefined : typeChoice);
    if (accepted) {
      setText('');
      setTypeChoice(AUTOMATIC);
    }
  };

  const submitOnShortcut = (event: KeyboardEvent) => {
    if (event.key === ENTER && (event.ctrlKey || event.metaKey)) {
      formRef.current?.requestSubmit();
    }
  };

  const AutomaticIcon = AUTOMATIC_ICON;
  const typeOptions = [
    {
      value: AUTOMATIC,
      label: t('quickInput.automatic'),
      icon: <AutomaticIcon size={CHIP_ICON_SIZE} aria-hidden="true" />,
    },
    ...ELEMENT_TYPE_ORDER.map((type) => {
      const Icon = ELEMENT_TYPE_ICONS[type];
      return { value: type, label: t(`elementType.${type}`), icon: <Icon size={CHIP_ICON_SIZE} aria-hidden="true" /> };
    }),
  ] as const;

  const numberFormat = new Intl.NumberFormat(i18n.language);
  const textError = textMissing
    ? t('quickInput.textMissing')
    : store.submitError === 'invalid'
      ? t('quickInput.invalid')
      : null;

  return (
    <section className="page">
      <h1 className="title">{t('quickInput.title')}</h1>
      <p className="subtitle is-6 has-text-grey">{t('quickInput.lead')}</p>
      <form ref={formRef} className="box quick-input" noValidate onSubmit={(event) => void submit(event)}>
        <div className="field">
          <label className="label" htmlFor={TEXT_ID}>
            {t('quickInput.textLabel')}
          </label>
          <div className="control">
            <textarea
              id={TEXT_ID}
              className={textError === null ? 'textarea' : 'textarea is-danger'}
              value={text}
              maxLength={MAX_TEXT_LENGTH}
              aria-describedby={TEXT_HELP_ID}
              aria-invalid={textError !== null}
              onChange={(event) => {
                setText(event.target.value);
                setTextMissing(false);
              }}
              onKeyDown={submitOnShortcut}
            />
          </div>
          <p
            id={TEXT_HELP_ID}
            className={textError === null ? 'help quick-input-help' : 'help is-danger quick-input-help'}
          >
            <span>{textError ?? t('quickInput.textHelp')}</span>
            <span>
              {numberFormat.format(text.length)} / {numberFormat.format(MAX_TEXT_LENGTH)}
            </span>
          </p>
        </div>
        <div className="field">
          <span className="label">{t('quickInput.typeLabel')}</span>
          <ChipGroup
            label={t('quickInput.typeLabel')}
            options={typeOptions}
            value={typeChoice}
            onChange={setTypeChoice}
          />
        </div>
        {store.submitError === 'failed' && (
          <p className="notification is-danger is-light" role="alert">
            {t('quickInput.submitFailed')}
          </p>
        )}
        <div className="quick-input-actions">
          <span className="is-size-7 has-text-grey">{t('quickInput.shortcut')}</span>
          <button type="submit" className={store.submitting ? 'button is-primary is-loading' : 'button is-primary'}>
            <Send size={CHIP_ICON_SIZE} aria-hidden="true" />
            <span>{t('quickInput.submit')}</span>
          </button>
        </div>
      </form>
      <InboundEventList store={store} />
    </section>
  );
});
