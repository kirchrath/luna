import type { ReactNode } from 'react';
import { NavLink } from 'react-router';

export interface SidebarItem {
  to: string;
  label: string;
  icon: ReactNode;
  tag?: string;
}

export interface SidebarGroup {
  title?: string;
  items: readonly SidebarItem[];
}

interface SidebarProps {
  id: string;
  label: string;
  brand: ReactNode;
  groups: readonly SidebarGroup[];
  collapsed: boolean;
  onToggleCollapsed: () => void;
  collapseLabel: string;
  expandLabel: string;
  collapseIcon: ReactNode;
  expandIcon: ReactNode;
  onNavigate: () => void;
}

export function Sidebar({
  id,
  label,
  brand,
  groups,
  collapsed,
  onToggleCollapsed,
  collapseLabel,
  expandLabel,
  collapseIcon,
  expandIcon,
  onNavigate,
}: SidebarProps) {
  const toggleLabel = collapsed ? expandLabel : collapseLabel;
  return (
    <nav id={id} className="sidebar" aria-label={label}>
      <div className="sidebar-brand">{brand}</div>
      {groups.map((group, index) => (
        <ul key={group.title ?? index} className="sidebar-group">
          {group.title !== undefined && <li className="sidebar-title">{group.title}</li>}
          {group.items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end
                className="sidebar-item"
                aria-label={item.label}
                data-tooltip={item.label}
                onClick={onNavigate}
              >
                {item.icon}
                <span className="sidebar-label">{item.label}</span>
                {item.tag !== undefined && <span className="tag is-rounded sidebar-tag">{item.tag}</span>}
              </NavLink>
            </li>
          ))}
        </ul>
      ))}
      <button
        type="button"
        className="sidebar-item sidebar-toggle"
        aria-expanded={!collapsed}
        aria-label={toggleLabel}
        data-tooltip={toggleLabel}
        onClick={onToggleCollapsed}
      >
        {collapsed ? expandIcon : collapseIcon}
        <span className="sidebar-label">{toggleLabel}</span>
      </button>
    </nav>
  );
}
