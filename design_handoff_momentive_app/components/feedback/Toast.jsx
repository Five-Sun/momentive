export function Toast({ message, visible }) {
  return (
    <div style={{
      position: 'absolute', left: '50%', bottom: 24, transform: `translate(-50%, ${visible ? '0' : '14px'}) scale(${visible ? 1 : .92})`,
      opacity: visible ? 1 : 0, transition: 'transform .32s var(--ease-spring), opacity .2s ease', pointerEvents: 'none',
      background: 'var(--ink)', color: '#fff', padding: '12px 20px', borderRadius: 'var(--radius-full)',
      font: 'var(--text-body-sm)', boxShadow: 'var(--shadow-float)', whiteSpace: 'nowrap',
    }}>{message}</div>
  );
}
