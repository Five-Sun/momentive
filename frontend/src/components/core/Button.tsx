import type { ReactNode } from "react";

interface ButtonProps {
  variant?: "primary" | "secondary" | "ghost";
  size?: "md" | "sm";
  disabled?: boolean;
  icon?: ReactNode;
  children: ReactNode;
  onClick?: () => void;
}

const sizeClasses = {
  md: "h-12 px-6 text-[15px]",
  sm: "h-[38px] px-[18px] text-[13px]",
};

const variantClasses = {
  primary: "bg-brand-pink text-on-brand disabled:bg-brand-pink-soft disabled:text-muted-soft",
  secondary: "bg-surface-card text-ink border-[1.5px] border-ink",
  ghost: "bg-transparent text-ink underline",
};

export function Button({
  variant = "primary",
  size = "md",
  disabled = false,
  icon,
  children,
  onClick,
}: ButtonProps) {
  return (
    <button
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-colors disabled:cursor-not-allowed ${sizeClasses[size]} ${variantClasses[variant]}`}
    >
      {icon}
      {children}
    </button>
  );
}
