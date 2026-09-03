import type { ReactNode } from "react";
import { Heart } from "lucide-react";

interface ProductCardProps {
  image: ReactNode;
  title: string;
  price: string;
  originalPrice?: string;
  badge?: ReactNode;
  favorited: boolean;
  onToggleFavorite?: () => void;
  rating?: ReactNode;
}

export function ProductCard({
  image,
  title,
  price,
  originalPrice,
  badge,
  favorited,
  onToggleFavorite,
  rating,
}: ProductCardProps) {
  return (
    <div className="flex w-full flex-col gap-2">
      <div className="bg-surface-strong relative aspect-square overflow-hidden rounded-2xl">
        {image}
        {badge && <div className="absolute top-2.5 left-2.5">{badge}</div>}
        <div className="absolute top-2 right-2">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleFavorite?.();
            }}
            className={`shadow-card flex h-8 w-8 items-center justify-center rounded-full bg-white/90 ${
              favorited ? "text-brand-pink-active" : "text-muted"
            }`}
          >
            <Heart
              key={String(favorited)}
              className="animate-paw-pop h-4 w-4"
              fill={favorited ? "currentColor" : "none"}
            />
          </button>
        </div>
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-title-sm text-ink">{title}</span>
        {rating}
        <div className="flex items-baseline gap-1.5">
          {originalPrice && (
            <span className="text-body-sm text-muted-soft line-through">{originalPrice}</span>
          )}
          <span className="text-price text-ink">{price}</span>
        </div>
      </div>
    </div>
  );
}
