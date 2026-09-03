export function SizeSelector({ sizes, selected, onSelect }) {
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      {sizes.map(s => {
        const isSel = s === selected;
        return (
          <button key={s} onClick={() => onSelect && onSelect(s)} style={{
            minWidth: 44, height: 44, padding: '0 4px', borderRadius: 'var(--radius-sm)',
            border: isSel ? '2px solid var(--ink)' : '1.5px solid var(--hairline)',
            background: isSel ? 'var(--ink)' : 'var(--surface-card)',
            color: isSel ? '#fff' : 'var(--ink)', font: 'var(--text-title-sm)', cursor: 'pointer',
          }}>{s}</button>
        );
      })}
    </div>
  );
}
