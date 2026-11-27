import { CircleUser, LogOut, Menu, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from 'react-oidc-context';
import { Link, Outlet } from 'react-router';

import { changeLanguage, LANGUAGES, type Language } from '../i18n';
import { AREAS, PATHS, QUICK_INPUT_ICON } from '../navigation';
import { readPreference, STORAGE_KEYS, writePreference } from '../services';
import { AppShell, LanguageSwitch, Logo, UserMenu } from '../ui';
import { LifeAreaSwitch } from './LifeAreaSwitch';

const ICON_SIZE = 20;
const COLLAPSED = 'true';

export function AppLayout() {
  const { t, i18n } = useTranslation();
  const auth = useAuth();
  const profile = auth.user?.profile;
  const userName = profile?.given_name ?? profile?.preferred_username ?? '';
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
          <LifeAreaSwitch />
          <LanguageSwitch
            label={t('language.label')}
            options={languageOptions}
            value={i18n.language as Language}
            onChange={(language) => void changeLanguage(language)}
          />
          <UserMenu
            label={t('user.menu')}
            name={userName}
            icon={<CircleUser size={ICON_SIZE} aria-hidden="true" />}
            logoutLabel={t('auth.logout')}
            logoutIcon={<LogOut size={16} aria-hidden="true" />}
            onLogout={() => void auth.signoutRedirect()}
          />
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
          items: AREAS.map(({ area, icon: Icon, available }) => ({
            to: PATHS[area],
            label: t(`navigation.${area}`),
            icon: <Icon size={ICON_SIZE} aria-hidden="true" />,
            ...(available ? {} : { tag: t('navigation.soon') }),
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
