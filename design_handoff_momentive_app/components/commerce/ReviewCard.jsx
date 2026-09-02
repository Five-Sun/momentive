export function ReviewCard({ author, rating, date, text, photoCount = 0 }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, paddingBottom: 14, borderBottom: '1px solid var(--hairline)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ font: 'var(--text-caption)', color: 'var(--ink)', fontWeight: 700 }}>{author}</span>
          <span style={{ font: 'var(--text-caption)', color: 'var(--ink)' }}>{'★'.repeat(Math.round(rating))}</span>
        </div>
        <span style={{ font: 'var(--text-caption)', color: 'var(--muted)' }}>{date}</span>
      </div>
      <p style={{ margin: 0, font: 'var(--text-body-sm)', color: 'var(--body)' }}>{text}</p>
      {photoCount > 0 && (
        <div style={{ display: 'flex', gap: 6 }}>
          {Array.from({ length: photoCount }).map((_, i) => (
            <div key={i} style={{ width: 56, height: 56, borderRadius: 'var(--radius-xs)', background: 'var(--surface-strong)' }} />
          ))}
        </div>
      )}
    </div>
  );
}
