interface ShippingProgressProps {
  /** 무료배송까지 남은 금액(원). 0 이하면 조건 달성 */
  remaining: number;
  formatAmount: (n: number) => string;
}

export function ShippingProgress({ remaining, formatAmount }: ShippingProgressProps) {
  const done = remaining <= 0;
  const pct = done ? 100 : Math.max(6, 100 - Math.min(100, (remaining / 50000) * 100));

  return (
    <div className="bg-surface-soft flex flex-col gap-2 rounded-sm p-3.5">
      <span className="text-body-sm text-ink">
        {done ? "무료배송 조건을 달성했어요 🤍" : `${formatAmount(remaining)} 더 담으면 무료배송`}
      </span>
      <div className="bg-hairline h-1.5 overflow-hidden rounded-full">
        <div
          style={{ width: `${pct}%` }}
          className="bg-brand-pink h-full rounded-full transition-all duration-300"
        />
      </div>
    </div>
  );
}
