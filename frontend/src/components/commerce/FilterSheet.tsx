import { Check } from "lucide-react";

interface FilterSheetProps {
  open: boolean;
  sortOptions: string[];
  selected: string;
  onSelect: (option: string) => void;
  onApply: () => void;
  onClose: () => void;
}

export function FilterSheet({
  open,
  sortOptions,
  selected,
  onSelect,
  onApply,
  onClose,
}: FilterSheetProps) {
  if (!open) return null;

  return (
    <div className="absolute inset-0 z-40 flex flex-col justify-end">
      <button
        aria-label="닫기"
        onClick={onClose}
        className="bg-scrim absolute inset-0 cursor-default"
      />
      <div className="bg-surface-card relative z-10 flex flex-col gap-3.5 rounded-t-lg p-5">
        <span className="text-title-sm text-ink">정렬</span>
        <div className="flex flex-col gap-0.5">
          {sortOptions.map((option) => {
            const isSelected = option === selected;
            return (
              <button
                key={option}
                onClick={() => onSelect(option)}
                className={`text-body flex h-11 items-center justify-between ${
                  isSelected ? "text-brand-pink-active font-bold" : "text-ink"
                }`}
              >
                {option}
                {isSelected && <Check className="h-4 w-4" />}
              </button>
            );
          })}
        </div>
        <button
          onClick={onApply}
          className="bg-brand-pink text-on-brand text-button h-12 rounded-full"
        >
          적용하기
        </button>
      </div>
    </div>
  );
}
