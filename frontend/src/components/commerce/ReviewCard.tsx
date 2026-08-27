import { Star } from "lucide-react";

interface ReviewCardProps {
  author: string;
  rating: number;
  date: string;
  text: string;
  photoCount?: number;
}

export function ReviewCard({ author, rating, date, text, photoCount = 0 }: ReviewCardProps) {
  return (
    <div className="border-hairline flex flex-col gap-1.5 border-b pb-3.5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-caption text-ink font-bold">{author}</span>
          <div className="text-ink flex items-center gap-0.5">
            {Array.from({ length: Math.round(rating) }).map((_, i) => (
              <Star key={i} className="h-3 w-3" fill="currentColor" />
            ))}
          </div>
        </div>
        <span className="text-caption text-muted">{date}</span>
      </div>
      <p className="text-body-sm text-body m-0">{text}</p>
      {photoCount > 0 && (
        <div className="flex gap-1.5">
          {Array.from({ length: photoCount }).map((_, i) => (
            <div key={i} className="bg-surface-strong rounded-xs h-14 w-14" />
          ))}
        </div>
      )}
    </div>
  );
}
