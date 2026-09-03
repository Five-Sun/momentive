export function ShippingProgress({ remaining, formatAmount }) {
  const done = remaining <= 0;
  const pct = done ? 100 : Math.max(6, 100 - Math.min(100, (remaining / 50000) * 100));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--surface-soft)' }}>
      <span style={{ font: 'var(--text-body-sm)', color: 'var(--ink)' }}>
        {done ? '무료배송 조건을 달성했어요 🤍' : `${formatAmount(remaining)} 더 담으면 무료배송`}
      </span>
      <div style={{ height: 6, borderRadius: 'var(--radius-full)', background: 'var(--hairline)', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${pct}%`, background: 'var(--brand-pink)', borderRadius: 'var(--radius-full)', transition: 'width .3s ease' }} />
      </div>
    </div>
  );
}
