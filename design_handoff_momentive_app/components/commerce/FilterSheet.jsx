export function FilterSheet({ open, sortOptions, selected, onSelect, onApply, onClose }) {
  if (!open) return null;
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 40, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'var(--scrim)' }} />
      <div style={{ position: 'relative', background: 'var(--surface-card)', borderRadius: 'var(--radius-lg) var(--radius-lg) 0 0', padding: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <span style={{ font: 'var(--text-title-sm)', color: 'var(--ink)' }}>정렬</span>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {sortOptions.map(opt => (
            <button key={opt} onClick={() => onSelect(opt)} style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 44, background: 'none', border: 'none',
              font: 'var(--text-body)', color: opt === selected ? 'var(--brand-pink-active)' : 'var(--ink)', fontWeight: opt === selected ? 700 : 400, cursor: 'pointer',
            }}>{opt}{opt === selected && <span>✓</span>}</button>
          ))}
        </div>
        <button onClick={onApply} style={{ height: 48, borderRadius: 'var(--radius-full)', border: 'none', background: 'var(--brand-pink)', color: 'var(--on-brand)', font: 'var(--text-button)', cursor: 'pointer' }}>적용하기</button>
      </div>
    </div>
  );
}
