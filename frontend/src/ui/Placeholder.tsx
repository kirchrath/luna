import type { ReactNode } from 'react';

interface PlaceholderProps {
  icon: ReactNode;
  title: string;
  text: string;
}

export function Placeholder({ icon, title, text }: PlaceholderProps) {
  return (
    <section className="placeholder has-text-centered">
      <span className="placeholder-icon">{icon}</span>
      <h1 className="title">{title}</h1>
      <p className="has-text-grey">{text}</p>
    </section>
  );
}
