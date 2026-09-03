// Preview-only shim: mirrors components/**/*.jsx as window.Momentive so this
// project's ui_kits/cards render standalone in this sandbox. The real
// _ds_bundle.js (compiled by the design-system pipeline from the .jsx sources
// themselves) is what other projects should consume — do not hand-maintain
// that file. Keep this shim's logic in sync with components/**/*.jsx.
(function () {
  const h = React.createElement;

  function Button({ variant = 'primary', size = 'md', disabled = false, icon = null, children, onClick }) {
    const sizes = { md: { height: 48, padding: '0 24px', fontSize: 15 }, sm: { height: 38, padding: '0 18px', fontSize: 13 } };
    const variants = {
      primary: { background: disabled ? 'var(--brand-pink-soft)' : 'var(--brand-pink)', color: disabled ? 'var(--muted-soft)' : 'var(--on-brand)', border: 'none' },
      secondary: { background: 'var(--surface-card)', color: 'var(--ink)', border: '1.5px solid var(--ink)' },
      ghost: { background: 'transparent', color: 'var(--ink)', border: 'none', textDecoration: 'underline' },
    };
    const s = sizes[size], v = variants[variant];
    return h('button', { onClick: disabled ? undefined : onClick, disabled, style: { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8, height: s.height, padding: s.padding, fontSize: s.fontSize, fontFamily: 'var(--font-body)', fontWeight: 600, borderRadius: 'var(--radius-full)', cursor: disabled ? 'not-allowed' : 'pointer', ...v } }, icon, children);
  }

  function IconButton({ children, active = false, size = 40, onClick, variant = 'outline' }) {
    const bg = variant === 'filled' ? 'var(--surface-strong)' : 'var(--surface-card)';
    return h('button', { onClick, style: { width: size, height: size, borderRadius: 'var(--radius-full)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', background: bg, border: variant === 'outline' ? '1px solid var(--hairline)' : 'none', color: active ? 'var(--brand-pink)' : 'var(--ink)', cursor: 'pointer', boxShadow: variant === 'filled' ? 'var(--shadow-card)' : 'none' } }, children);
  }

  function Badge({ label, tone = 'new' }) {
    const tones = { new: { background: 'var(--ink)', color: '#fff' }, sale: { background: 'var(--sale)', color: '#fff' }, soldout: { background: 'var(--surface-strong)', color: 'var(--muted)' }, neutral: { background: 'var(--brand-yellow)', color: 'var(--ink)' } };
    return h('span', { style: { display: 'inline-flex', alignItems: 'center', padding: '4px 10px', borderRadius: 'var(--radius-full)', font: 'var(--text-tag)', letterSpacing: '.2px', ...tones[tone] } }, label);
  }

  function Chip({ label, selected = false, onClick }) {
    const wasSelected = React.useRef(selected);
    const [pop, setPop] = React.useState(false);
    React.useEffect(() => {
      if (selected && !wasSelected.current) { setPop(true); const t = setTimeout(() => setPop(false), 260); wasSelected.current = true; return () => clearTimeout(t); }
      wasSelected.current = selected;
    }, [selected]);
    return h('button', { onClick, style: { height: 36, padding: '0 16px', borderRadius: 'var(--radius-full)', border: selected ? '1.5px solid var(--ink)' : '1.5px solid var(--hairline)', background: selected ? 'var(--ink)' : 'var(--surface-card)', color: selected ? '#fff' : 'var(--ink)', font: 'var(--text-caption)', cursor: 'pointer', whiteSpace: 'nowrap', transition: 'background .15s, border-color .15s', animation: pop ? 'paw-pop .26s var(--ease-spring)' : 'none' } }, label);
  }

  function SearchInput({ value, onChange, placeholder = '검색어를 입력하세요' }) {
    return h('div', { style: { display: 'flex', alignItems: 'center', gap: 8, height: 48, padding: '0 18px', borderRadius: 'var(--radius-full)', background: 'var(--surface-soft)', border: '1px solid var(--hairline)' } },
      h('span', { style: { color: 'var(--muted)' } }, '⌕'),
      h('input', { value, onChange: e => onChange && onChange(e.target.value), placeholder, style: { border: 'none', outline: 'none', background: 'transparent', flex: 1, font: 'var(--text-body)', color: 'var(--ink)' } })
    );
  }

  function BottomNav({ items, activeKey, onSelect }) {
    return h('div', { style: { display: 'flex', justifyContent: 'space-around', alignItems: 'center', height: 64, background: 'var(--surface-card)', borderTop: '1px solid var(--hairline)' } },
      items.map(it => {
        const active = it.key === activeKey;
        return h('button', { key: it.key, onClick: () => onSelect && onSelect(it.key), style: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, background: 'none', border: 'none', cursor: 'pointer', color: active ? 'var(--brand-pink-active)' : 'var(--muted)' } },
          h('span', { key: active ? it.key + '-a' : it.key, style: { fontSize: 20, display: 'inline-block', animation: active ? 'bump-up .35s var(--ease-spring)' : 'none' } }, it.icon),
          h('span', { style: { font: 'var(--text-caption)', fontWeight: active ? 700 : 500 } }, it.label)
        );
      })
    );
  }

  function Toast({ message, visible }) {
    return h('div', { style: { position: 'absolute', left: '50%', bottom: 24, transform: `translate(-50%, ${visible ? '0' : '14px'}) scale(${visible ? 1 : .92})`, opacity: visible ? 1 : 0, transition: 'transform .32s var(--ease-spring), opacity .2s ease', pointerEvents: 'none', background: 'var(--ink)', color: '#fff', padding: '12px 20px', borderRadius: 'var(--radius-full)', font: 'var(--text-body-sm)', boxShadow: 'var(--shadow-float)', whiteSpace: 'nowrap' } }, message);
  }

  function Rating({ value, count }) {
    return h('div', { style: { display: 'flex', alignItems: 'center', gap: 4, color: 'var(--ink)' } },
      h('span', { style: { fontSize: 13 } }, '★'),
      h('span', { style: { font: 'var(--text-caption)', fontWeight: 700 } }, value.toFixed(1)),
      count != null && h('span', { style: { font: 'var(--text-caption)', color: 'var(--muted)', fontWeight: 400 } }, `(${count})`)
    );
  }

  function SizeSelector({ sizes, selected, onSelect }) {
    return h('div', { style: { display: 'flex', gap: 8, flexWrap: 'wrap' } },
      sizes.map(s => {
        const isSel = s === selected;
        return h('button', { key: s, onClick: () => onSelect && onSelect(s), style: { minWidth: 44, height: 44, padding: '0 4px', borderRadius: 'var(--radius-sm)', border: isSel ? '2px solid var(--ink)' : '1.5px solid var(--hairline)', background: isSel ? 'var(--ink)' : 'var(--surface-card)', color: isSel ? '#fff' : 'var(--ink)', font: 'var(--text-title-sm)', cursor: 'pointer' } }, s);
      })
    );
  }

  function ProductCard({ image, title, price, originalPrice, badge, favorited, onToggleFavorite, rating }) {
    const [pop, setPop] = React.useState(false);
    const handleFav = () => { setPop(true); onToggleFavorite && onToggleFavorite(); setTimeout(() => setPop(false), 320); };
    return h('div', { style: { display: 'flex', flexDirection: 'column', gap: 8, width: '100%' } },
      h('div', { style: { position: 'relative', aspectRatio: '1', borderRadius: 'var(--radius-md)', overflow: 'hidden', background: 'var(--surface-strong)' } },
        image,
        badge && h('div', { style: { position: 'absolute', top: 10, left: 10 } }, badge),
        h('div', { style: { position: 'absolute', top: 8, right: 8 } },
          h('button', { onClick: handleFav, style: { width: 32, height: 32, borderRadius: 'var(--radius-full)', border: 'none', background: 'rgba(255,255,255,.9)', boxShadow: 'var(--shadow-card)', color: favorited ? 'var(--brand-pink-active)' : 'var(--muted)', cursor: 'pointer', animation: pop ? 'paw-pop .32s var(--ease-spring)' : 'none' } }, '♥')
        )
      ),
      h('div', { style: { display: 'flex', flexDirection: 'column', gap: 4 } },
        h('span', { style: { font: 'var(--text-title-sm)', color: 'var(--ink)' } }, title),
        rating != null && h('div', null, rating),
        h('div', { style: { display: 'flex', alignItems: 'baseline', gap: 6 } },
          originalPrice && h('span', { style: { font: 'var(--text-body-sm)', color: 'var(--muted-soft)', textDecoration: 'line-through' } }, originalPrice),
          h('span', { style: { font: 'var(--text-price)', color: 'var(--ink)' } }, price)
        )
      )
    );
  }

  function FilterSheet({ open, sortOptions, selected, onSelect, onApply, onClose }) {
    if (!open) return null;
    return h('div', { style: { position: 'absolute', inset: 0, zIndex: 40, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' } },
      h('div', { onClick: onClose, style: { position: 'absolute', inset: 0, background: 'var(--scrim)' } }),
      h('div', { style: { position: 'relative', background: 'var(--surface-card)', borderRadius: 'var(--radius-lg) var(--radius-lg) 0 0', padding: 20, display: 'flex', flexDirection: 'column', gap: 14 } },
        h('span', { style: { font: 'var(--text-title-sm)', color: 'var(--ink)' } }, '정렬'),
        h('div', { style: { display: 'flex', flexDirection: 'column', gap: 2 } },
          sortOptions.map(opt => h('button', { key: opt, onClick: () => onSelect(opt), style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 44, background: 'none', border: 'none', font: 'var(--text-body)', color: opt === selected ? 'var(--brand-pink-active)' : 'var(--ink)', fontWeight: opt === selected ? 700 : 400, cursor: 'pointer' } }, opt, opt === selected && h('span', null, '✓')))
        ),
        h('button', { onClick: onApply, style: { height: 48, borderRadius: 'var(--radius-full)', border: 'none', background: 'var(--brand-pink)', color: 'var(--on-brand)', font: 'var(--text-button)', cursor: 'pointer' } }, '적용하기')
      )
    );
  }

  function ReviewCard({ author, rating, date, text, photoCount = 0 }) {
    return h('div', { style: { display: 'flex', flexDirection: 'column', gap: 6, paddingBottom: 14, borderBottom: '1px solid var(--hairline)' } },
      h('div', { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' } },
        h('div', { style: { display: 'flex', alignItems: 'center', gap: 8 } },
          h('span', { style: { font: 'var(--text-caption)', color: 'var(--ink)', fontWeight: 700 } }, author),
          h('span', { style: { font: 'var(--text-caption)', color: 'var(--ink)' } }, '★'.repeat(Math.round(rating)))
        ),
        h('span', { style: { font: 'var(--text-caption)', color: 'var(--muted)' } }, date)
      ),
      h('p', { style: { margin: 0, font: 'var(--text-body-sm)', color: 'var(--body)' } }, text),
      photoCount > 0 && h('div', { style: { display: 'flex', gap: 6 } }, Array.from({ length: photoCount }).map((_, i) => h('div', { key: i, style: { width: 56, height: 56, borderRadius: 'var(--radius-xs)', background: 'var(--surface-strong)' } })))
    );
  }

  function ShippingProgress({ remaining, formatAmount }) {
    const done = remaining <= 0;
    const pct = done ? 100 : Math.max(6, 100 - Math.min(100, (remaining / 50000) * 100));
    return h('div', { style: { display: 'flex', flexDirection: 'column', gap: 8, padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--surface-soft)' } },
      h('span', { style: { font: 'var(--text-body-sm)', color: 'var(--ink)' } }, done ? '무료배송 조건을 달성했어요 🤍' : `${formatAmount(remaining)} 더 담으면 무료배송`),
      h('div', { style: { height: 6, borderRadius: 'var(--radius-full)', background: 'var(--hairline)', overflow: 'hidden' } },
        h('div', { style: { height: '100%', width: `${pct}%`, background: 'var(--brand-pink)', borderRadius: 'var(--radius-full)', transition: 'width .3s ease' } })
      )
    );
  }

  window.Momentive = { Button, IconButton, Badge, Chip, SearchInput, BottomNav, Toast, Rating, SizeSelector, ProductCard, FilterSheet, ReviewCard, ShippingProgress };
})();
