export function Badge({ label, tone = 'new' }) {
  const tones = {
    new: { background: 'var(--ink)', color: '#fff' },
    sale: { background: 'var(--sale)', color: '#fff' },
    soldout: { background: 'var(--surface-strong)', color: 'var(--muted)' },
    neutral: { background: 'var(--brand-yellow)', color: 'var(--ink)' },
  };
  const t = tones[tone];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', padding: '4px 10px',
      borderRadius: 'var(--radius-full)', font: 'var(--text-tag)', letterSpacing: '.2px',
      ...t,
    }}>{label}</span>
  );
}
