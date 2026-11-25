import { LoaderCircle } from 'lucide-react';
import { useEffect, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from 'react-oidc-context';

import { StatusScreen } from '../ui';
import { createSignInState } from './oidcConfig';

const ICON_SIZE = 32;

interface AuthGateProps {
  children: ReactNode;
}

export function AuthGate({ children }: AuthGateProps) {
  const { t } = useTranslation();
  const auth = useAuth();
  const mustSignIn = !auth.isLoading && !auth.isAuthenticated && auth.activeNavigator === undefined && !auth.error;

  useEffect(() => {
    if (mustSignIn) {
      void auth.signinRedirect({ state: createSignInState() });
    }
  }, [mustSignIn, auth]);

  if (auth.error) {
    return (
      <StatusScreen
        title={t('auth.errorTitle')}
        text={t('auth.errorText')}
        action={{ label: t('auth.retry'), onClick: () => void auth.signinRedirect({ state: createSignInState() }) }}
      />
    );
  }

  if (!auth.isAuthenticated) {
    return (
      <StatusScreen title={t('auth.signingIn')}>
        <LoaderCircle className="status-spinner" size={ICON_SIZE} aria-hidden="true" />
      </StatusScreen>
    );
  }

  return children;
}
