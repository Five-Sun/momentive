export function IconButton({ children, active = false, size = 40, onClick, variant = 'outline' }) {
  const bg = variant === 'filled' ? 'var(--surface-strong)' : 'var(--surface-card)';
  return (
    <button
      onClick={onClick}
      style={{
        width: size, height: size, borderRadius: 'var(--radius-full)',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        background: bg, border: variant === 'outline' ? '1px solid var(--hairline)' : 'none',
        color: active ? 'var(--brand-pink)' : 'var(--ink)', cursor: 'pointer',
        boxShadow: variant === 'filled' ? 'var(--shadow-card)' : 'none',
      }}
    >
      {children}
    </button>
  );
}
