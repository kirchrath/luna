import type { ReactNode } from 'react';

export interface SegmentOption<V extends string> {
  value: V;
  label: string;
  /** With an icon, the label may be hidden on small screens; it is still read out. */
  icon?: ReactNode;
}

interface SegmentedControlProps<V extends string> {
  label: string;
  options: readonly SegmentOption<V>[];
  value: V;
  onChange: (value: V) => void;
  className?: string;
}

export function SegmentedControl<V extends string>({
  label,
  options,
  value,
  onChange,
  className,
}: SegmentedControlProps<V>) {
  const classNames = ['buttons', 'has-addons', 'segmented-control'];
  if (className !== undefined) classNames.push(className);
  return (
    <div className={classNames.join(' ')} role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={option.value === value ? 'button is-small is-primary' : 'button is-small'}
          aria-pressed={option.value === value}
          aria-label={option.icon === undefined ? undefined : option.label}
          title={option.icon === undefined ? undefined : option.label}
          onClick={() => {
            onChange(option.value);
          }}
        >
          {option.icon}
          {option.icon === undefined ? option.label : <span className="segment-label">{option.label}</span>}
        </button>
      ))}
    </div>
  );
}
