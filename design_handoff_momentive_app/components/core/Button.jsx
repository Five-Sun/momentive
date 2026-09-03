export function Button({ variant = 'primary', size = 'md', disabled = false, icon = null, children, onClick }) {
  const sizes = {
    md: { height: 48, padding: '0 24px', fontSize: 15 },
    sm: { height: 38, padding: '0 18px', fontSize: 13 },
  };
  const variants = {
    primary: { background: disabled ? 'var(--brand-pink-soft)' : 'var(--brand-pink)', color: disabled ? 'var(--muted-soft)' : 'var(--on-brand)', border: 'none' },
    secondary: { background: 'var(--surface-card)', color: 'var(--ink)', border: '1.5px solid var(--ink)' },
    ghost: { background: 'transparent', color: 'var(--ink)', border: 'none', textDecoration: 'underline' },
  };
  const s = sizes[size];
  const v = variants[variant];
  return (
    <button
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        height: s.height, padding: s.padding, fontSize: s.fontSize,
        fontFamily: 'var(--font-body)', fontWeight: 600, borderRadius: 'var(--radius-full)',
        cursor: disabled ? 'not-allowed' : 'pointer', transition: 'background .15s ease',
        ...v,
      }}
    >
      {icon}
      {children}
    </button>
  );
}
