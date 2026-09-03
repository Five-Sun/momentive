export function BottomNav({ items, activeKey, onSelect }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-around', alignItems: 'center',
      height: 64, background: 'var(--surface-card)', borderTop: '1px solid var(--hairline)',
    }}>
      {items.map(it => {
        const active = it.key === activeKey;
        return (
          <button key={it.key} onClick={() => onSelect && onSelect(it.key)} style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
            background: 'none', border: 'none', cursor: 'pointer',
            color: active ? 'var(--brand-pink-active)' : 'var(--muted)',
          }}>
            <span key={active ? it.key + '-a' : it.key} style={{ fontSize: 20, display: 'inline-block', animation: active ? 'bump-up .35s var(--ease-spring)' : 'none' }}>{it.icon}</span>
            <span style={{ font: 'var(--text-caption)', fontWeight: active ? 700 : 500 }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}
