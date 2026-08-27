import { Star } from "lucide-react";

interface RatingProps {
  value: number;
  count?: number;
}

export function Rating({ value, count }: RatingProps) {
  return (
    <div className="text-ink flex items-center gap-1">
      <Star className="h-3.5 w-3.5" fill="currentColor" />
      <span className="text-caption font-bold">{value.toFixed(1)}</span>
      {count != null && <span className="text-caption text-muted font-normal">({count})</span>}
    </div>
  );
}
