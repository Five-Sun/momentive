import { Star } from "lucide-react";

interface ReviewCardProps {
  authorNickname: string;
  rating: number;
  createdAt: string;
  text: string;
  isMine?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}

function formatDate(iso: string) {
  return iso.slice(0, 10).replace(/-/g, ".");
}

export function ReviewCard({
  authorNickname,
  rating,
  createdAt,
  text,
  isMine = false,
  onEdit,
  onDelete,
}: ReviewCardProps) {
  return (
    <div className="border-hairline flex flex-col gap-1.5 border-b pb-3.5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-caption text-ink font-bold">{authorNickname}</span>
          <div className="text-ink flex items-center gap-0.5">
            {Array.from({ length: Math.round(rating) }).map((_, i) => (
              <Star key={i} className="h-3 w-3" fill="currentColor" />
            ))}
          </div>
        </div>
        <span className="text-caption text-muted">{formatDate(createdAt)}</span>
      </div>
      <p className="text-body-sm text-body m-0">{text}</p>
      {isMine && (
        <div className="flex gap-3">
          <button onClick={onEdit} className="text-caption text-body underline">
            수정
          </button>
          <button onClick={onDelete} className="text-caption text-error underline">
            삭제
          </button>
        </div>
      )}
    </div>
  );
}
