export function SearchInput({ value, onChange, placeholder = '검색어를 입력하세요' }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8, height: 48, padding: '0 18px',
      borderRadius: 'var(--radius-full)', background: 'var(--surface-soft)', border: '1px solid var(--hairline)',
    }}>
      <span style={{ color: 'var(--muted)' }}>⌕</span>
      <input
        value={value}
        onChange={e => onChange && onChange(e.target.value)}
        placeholder={placeholder}
        style={{ border: 'none', outline: 'none', background: 'transparent', flex: 1, font: 'var(--text-body)', color: 'var(--ink)' }}
      />
    </div>
  );
}
