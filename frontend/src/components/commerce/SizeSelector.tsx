interface SizeSelectorProps {
  sizes: string[];
  selected: string;
  onSelect?: (size: string) => void;
}

export function SizeSelector({ sizes, selected, onSelect }: SizeSelectorProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {sizes.map((size) => {
        const isSelected = size === selected;
        return (
          <button
            key={size}
            onClick={() => onSelect?.(size)}
            className={`text-title-sm h-11 min-w-11 rounded-[10px] px-1 ${
              isSelected
                ? "border-2 border-ink bg-ink text-white"
                : "border-[1.5px] border-hairline bg-surface-card text-ink"
            }`}
          >
            {size}
          </button>
        );
      })}
    </div>
  );
}
