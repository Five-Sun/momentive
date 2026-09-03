export function Rating({ value, count }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--ink)' }}>
      <span style={{ fontSize: 13 }}>★</span>
      <span style={{ font: 'var(--text-caption)', fontWeight: 700 }}>{value.toFixed(1)}</span>
      {count != null && <span style={{ font: 'var(--text-caption)', color: 'var(--muted)', fontWeight: 400 }}>({count})</span>}
    </div>
  );
}
