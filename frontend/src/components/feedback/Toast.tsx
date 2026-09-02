interface ToastProps {
  message: string;
  visible: boolean;
}

export function Toast({ message, visible }: ToastProps) {
  return (
    <div
      style={{
        transform: `translateX(-50%) translateY(${visible ? "0" : "12px"}) scale(${visible ? 1 : 0.9})`,
        opacity: visible ? 1 : 0,
        transitionTimingFunction: "var(--ease-spring)",
      }}
      className="text-body-sm shadow-float bg-ink pointer-events-none absolute bottom-6 left-1/2 whitespace-nowrap rounded-full px-5 py-3 text-white transition-all duration-300"
    >
      {message}
    </div>
  );
}
