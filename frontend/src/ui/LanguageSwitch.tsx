interface LanguageOption<L extends string> {
  value: L;
  short: string;
  label: string;
}

interface LanguageSwitchProps<L extends string> {
  label: string;
  options: readonly LanguageOption<L>[];
  value: L;
  onChange: (value: L) => void;
}

export function LanguageSwitch<L extends string>({ label, options, value, onChange }: LanguageSwitchProps<L>) {
  return (
    <div className="buttons has-addons language-switch" role="group" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          className={option.value === value ? 'button is-small is-primary is-selected' : 'button is-small'}
          aria-pressed={option.value === value}
          aria-label={option.label}
          onClick={() => {
            onChange(option.value);
          }}
        >
          {option.short}
        </button>
      ))}
    </div>
  );
}
