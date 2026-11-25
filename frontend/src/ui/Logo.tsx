interface LogoProps {
  size?: number;
}

export function Logo({ size = 24 }: LogoProps) {
  return (
    <svg
      className="logo"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle className="logo-surface" cx="12" cy="12" r="9" />
      <circle cx="9" cy="9" r="2.2" />
      <circle cx="15" cy="14" r="2.8" />
      <circle cx="8.6" cy="15.6" r="1.2" />
      <circle cx="15.6" cy="7.6" r="0.9" />
    </svg>
  );
}
