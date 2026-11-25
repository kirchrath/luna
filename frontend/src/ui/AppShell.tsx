import { useEffect, useState, type ReactNode } from 'react';

import { Sidebar, type SidebarGroup } from './Sidebar';

const SIDEBAR_ID = 'app-sidebar';
const ESCAPE = 'Escape';

interface AppShellProps {
  brand: ReactNode;
  headerEnd: ReactNode;
  navigationLabel: string;
  groups: readonly SidebarGroup[];
  collapsed: boolean;
  onCollapsedChange: (collapsed: boolean) => void;
  labels: { open: string; close: string; collapse: string; expand: string };
  icons: { open: ReactNode; collapse: ReactNode; expand: ReactNode };
  children: ReactNode;
}

export function AppShell({
  brand,
  headerEnd,
  navigationLabel,
  groups,
  collapsed,
  onCollapsedChange,
  labels,
  icons,
  children,
}: AppShellProps) {
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    if (!mobileOpen) {
      return undefined;
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === ESCAPE) {
        setMobileOpen(false);
      }
    };
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [mobileOpen]);

  const classNames = ['app-shell'];
  if (collapsed) classNames.push('is-collapsed');
  if (mobileOpen) classNames.push('is-mobile-open');

  return (
    <div className={classNames.join(' ')}>
      <header className="app-header">
        <button
          type="button"
          className="button is-white app-burger"
          aria-label={labels.open}
          aria-controls={SIDEBAR_ID}
          aria-expanded={mobileOpen}
          onClick={() => {
            setMobileOpen(true);
          }}
        >
          {icons.open}
        </button>
        <div className="app-brand">{brand}</div>
        <div className="app-header-end">{headerEnd}</div>
      </header>
      <div className="app-body">
        <Sidebar
          id={SIDEBAR_ID}
          label={navigationLabel}
          brand={brand}
          groups={groups}
          collapsed={collapsed}
          onToggleCollapsed={() => {
            onCollapsedChange(!collapsed);
          }}
          collapseLabel={labels.collapse}
          expandLabel={labels.expand}
          collapseIcon={icons.collapse}
          expandIcon={icons.expand}
          onNavigate={() => {
            setMobileOpen(false);
          }}
        />
        {mobileOpen && (
          <button
            type="button"
            className="app-backdrop"
            aria-label={labels.close}
            onClick={() => {
              setMobileOpen(false);
            }}
          />
        )}
        <main className="app-content">{children}</main>
      </div>
    </div>
  );
}
