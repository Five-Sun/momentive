export function ProductCard({ image, title, price, originalPrice, badge, favorited, onToggleFavorite, rating }) {
  const [pop, setPop] = React.useState(false);
  const handleFav = () => {
    setPop(true); onToggleFavorite && onToggleFavorite(); setTimeout(() => setPop(false), 320);
  };
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
      <div style={{ position: 'relative', aspectRatio: '1', borderRadius: 'var(--radius-md)', overflow: 'hidden', background: 'var(--surface-strong)' }}>
        {image}
        {badge && <div style={{ position: 'absolute', top: 10, left: 10 }}>{badge}</div>}
        <div style={{ position: 'absolute', top: 8, right: 8 }}>
          <button onClick={handleFav} style={{
            width: 32, height: 32, borderRadius: 'var(--radius-full)', border: 'none',
            background: 'rgba(255,255,255,.9)', boxShadow: 'var(--shadow-card)',
            color: favorited ? 'var(--brand-pink-active)' : 'var(--muted)', cursor: 'pointer',
            animation: pop ? 'paw-pop .32s var(--ease-spring)' : 'none',
          }}>♥</button>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        <span style={{ font: 'var(--text-title-sm)', color: 'var(--ink)' }}>{title}</span>
        {rating != null && <div>{rating}</div>}
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
          {originalPrice && <span style={{ font: 'var(--text-body-sm)', color: 'var(--muted-soft)', textDecoration: 'line-through' }}>{originalPrice}</span>}
          <span style={{ font: 'var(--text-price)', color: 'var(--ink)' }}>{price}</span>
        </div>
      </div>
    </div>
  );
}
