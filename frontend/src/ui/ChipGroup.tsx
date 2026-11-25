import type { ReactNode } from 'react';

export interface ChipOption<V extends string> {
  value: V;
  label: string;
  icon: ReactNode;
}

interface ChipGroupProps<V extends string> {
  label: string;
  options: readonly ChipOption<V>[];
  value: V;
  onChange: (value: V) => void;
}

export function ChipGroup<V extends string>({ label, options, value, onChange }: ChipGroupProps<V>) {
  return (
    <div className="chip-group" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={
            option.value === value ? 'button is-small is-rounded is-primary chip' : 'button is-small is-rounded chip'
          }
          aria-pressed={option.value === value}
          onClick={() => {
            onChange(option.value);
          }}
        >
          {option.icon}
          <span>{option.label}</span>
        </button>
      ))}
    </div>
  );
}
