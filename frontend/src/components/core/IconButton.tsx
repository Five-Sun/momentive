import type { ReactNode } from "react";

interface IconButtonProps {
  children: ReactNode;
  active?: boolean;
  size?: number;
  onClick?: () => void;
  variant?: "outline" | "filled";
}

export function IconButton({
  children,
  active = false,
  size = 40,
  onClick,
  variant = "outline",
}: IconButtonProps) {
  return (
    <button
      onClick={onClick}
      style={{ width: size, height: size }}
      className={`inline-flex items-center justify-center rounded-full ${
        variant === "filled"
          ? "bg-surface-strong shadow-card"
          : "bg-surface-card border border-hairline"
      } ${active ? "text-brand-pink" : "text-ink"}`}
    >
      {children}
    </button>
  );
}
