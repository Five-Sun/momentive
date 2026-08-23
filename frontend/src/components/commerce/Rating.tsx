interface RatingProps {
  value: number;
  count?: number;
}

export function Rating({ value, count }: RatingProps) {
  return (
    <div className="text-ink flex items-center gap-1">
      <span className="text-[13px]">★</span>
      <span className="text-caption font-bold">{value.toFixed(1)}</span>
      {count != null && <span className="text-caption text-muted font-normal">({count})</span>}
    </div>
  );
}
