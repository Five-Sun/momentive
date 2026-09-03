interface BadgeProps {
  label: string;
  tone?: "new" | "sale" | "soldout" | "neutral";
}

const toneClasses = {
  new: "bg-ink text-white",
  sale: "bg-sale text-white",
  soldout: "bg-surface-strong text-muted",
  neutral: "bg-brand-yellow text-ink",
};

export function Badge({ label, tone = "new" }: BadgeProps) {
  return (
    <span
      className={`text-tag animate-paw-pop inline-flex items-center rounded-full px-2.5 py-1 tracking-[0.2px] ${toneClasses[tone]}`}
    >
      {label}
    </span>
  );
}
