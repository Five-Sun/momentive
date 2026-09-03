export function Chip({ label, selected = false, onClick }) {
  const wasSelected = React.useRef(selected);
  const [pop, setPop] = React.useState(false);
  React.useEffect(() => {
    if (selected && !wasSelected.current) {
      setPop(true);
      const t = setTimeout(() => setPop(false), 260);
      wasSelected.current = true;
      return () => clearTimeout(t);
    }
    wasSelected.current = selected;
  }, [selected]);
  return (
    <button onClick={onClick} style={{
      height: 36, padding: '0 16px', borderRadius: 'var(--radius-full)',
      border: selected ? '1.5px solid var(--ink)' : '1.5px solid var(--hairline)',
      background: selected ? 'var(--ink)' : 'var(--surface-card)',
      color: selected ? '#fff' : 'var(--ink)',
      font: 'var(--text-caption)', cursor: 'pointer', whiteSpace: 'nowrap',
      transition: 'background .15s, border-color .15s',
      animation: pop ? 'paw-pop .26s var(--ease-spring)' : 'none',
    }}>{label}</button>
  );
}
