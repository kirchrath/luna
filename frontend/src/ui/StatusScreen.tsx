import type { ReactNode } from 'react';

interface StatusScreenProps {
  title: string;
  text?: string;
  action?: { label: string; onClick: () => void };
  children?: ReactNode;
}

export function StatusScreen({ title, text, action, children }: StatusScreenProps) {
  return (
    <main className="status-screen has-text-centered">
      {children}
      <h1 className="title is-4">{title}</h1>
      {text !== undefined && <p className="has-text-grey">{text}</p>}
      {action !== undefined && (
        <button type="button" className="button is-primary" onClick={action.onClick}>
          {action.label}
        </button>
      )}
    </main>
  );
}
