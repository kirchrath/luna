import { CircleUser, Menu, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet } from 'react-router';

import { changeLanguage, LANGUAGES, type Language } from '../i18n';
import { AREAS, PATHS, QUICK_INPUT_ICON } from '../navigation';
import { readPreference, STORAGE_KEYS, writePreference } from '../services/storage';
import { AppShell } from '../ui/AppShell';
import { LanguageSwitch } from '../ui/LanguageSwitch';
import { Logo } from '../ui/Logo';

const ICON_SIZE = 20;
const COLLAPSED = 'true';

export function AppLayout() {
  const { t, i18n } = useTranslation();
  const [collapsed, setCollapsed] = useState(() => readPreference(STORAGE_KEYS.navigationCollapsed) === COLLAPSED);
  const QuickInputIcon = QUICK_INPUT_ICON;

  const brand = (
    <Link to={PATHS.quickInput} className="brand">
      <Logo />
      <span>{t('app.name')}</span>
    </Link>
  );

  const languageOptions = LANGUAGES.map((language) => ({
    value: language,
    short: language.toUpperCase(),
    label: t(`language.${language}`),
  }));

  return (
    <AppShell
      brand={brand}
      headerEnd={
        <>
          <LanguageSwitch
            label={t('language.label')}
            options={languageOptions}
            value={i18n.language as Language}
            onChange={(language) => void changeLanguage(language)}
          />
          <span className="user-menu" role="img" aria-label={t('user.menu')}>
            <CircleUser size={28} aria-hidden="true" />
          </span>
        </>
      }
      navigationLabel={t('navigation.label')}
      groups={[
        {
          items: [
            {
              to: PATHS.quickInput,
              label: t('navigation.quickInput'),
              icon: <QuickInputIcon size={ICON_SIZE} aria-hidden="true" />,
            },
          ],
        },
        {
          title: t('navigation.areas'),
          items: AREAS.map(({ area, icon: Icon }) => ({
            to: PATHS[area],
            label: t(`navigation.${area}`),
            icon: <Icon size={ICON_SIZE} aria-hidden="true" />,
            tag: t('navigation.soon'),
          })),
        },
      ]}
      collapsed={collapsed}
      onCollapsedChange={(value) => {
        writePreference(STORAGE_KEYS.navigationCollapsed, String(value));
        setCollapsed(value);
      }}
      labels={{
        open: t('navigation.open'),
        close: t('navigation.close'),
        collapse: t('navigation.collapse'),
        expand: t('navigation.expand'),
      }}
      icons={{
        open: <Menu size={ICON_SIZE} aria-hidden="true" />,
        collapse: <PanelLeftClose size={ICON_SIZE} aria-hidden="true" />,
        expand: <PanelLeftOpen size={ICON_SIZE} aria-hidden="true" />,
      }}
    >
      <Outlet />
    </AppShell>
  );
}
