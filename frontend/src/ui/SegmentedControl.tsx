export interface SegmentOption<V extends string> {
  value: V;
  label: string;
}

interface SegmentedControlProps<V extends string> {
  label: string;
  options: readonly SegmentOption<V>[];
  value: V;
  onChange: (value: V) => void;
}

export function SegmentedControl<V extends string>({ label, options, value, onChange }: SegmentedControlProps<V>) {
  return (
    <div className="buttons has-addons segmented-control" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={option.value === value ? 'button is-small is-primary' : 'button is-small'}
          aria-pressed={option.value === value}
          onClick={() => {
            onChange(option.value);
          }}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
