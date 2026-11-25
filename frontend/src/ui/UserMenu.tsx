import { useEffect, useRef, useState, type ReactNode } from 'react';

const ESCAPE = 'Escape';

interface UserMenuProps {
  label: string;
  name: string;
  icon: ReactNode;
  logoutLabel: string;
  logoutIcon: ReactNode;
  onLogout: () => void;
}

export function UserMenu({ label, name, icon, logoutLabel, logoutIcon, onLogout }: UserMenuProps) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === ESCAPE) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [open]);

  return (
    <div ref={menuRef} className={open ? 'dropdown is-right is-active user-menu' : 'dropdown is-right user-menu'}>
      <div className="dropdown-trigger">
        <button
          type="button"
          className="button is-white user-menu-button"
          aria-label={label}
          aria-haspopup="menu"
          aria-expanded={open}
          onClick={() => {
            setOpen(!open);
          }}
        >
          {icon}
          <span className="user-menu-name">{name}</span>
        </button>
      </div>
      {open && (
        <div className="dropdown-menu" role="menu">
          <div className="dropdown-content">
            <button type="button" role="menuitem" className="dropdown-item user-menu-item" onClick={onLogout}>
              {logoutIcon}
              <span>{logoutLabel}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
