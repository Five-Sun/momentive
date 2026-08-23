interface ToastProps {
  message: string;
  visible: boolean;
}

export function Toast({ message, visible }: ToastProps) {
  return (
    <div
      style={{
        transform: `translateX(-50%) translateY(${visible ? "0" : "12px"})`,
        opacity: visible ? 1 : 0,
      }}
      className="text-body-sm shadow-float bg-ink pointer-events-none absolute bottom-6 left-1/2 whitespace-nowrap rounded-full px-5 py-3 text-white transition-all duration-200"
    >
      {message}
    </div>
  );
}
