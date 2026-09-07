interface SizeSelectorProps {
  sizes: string[];
  selected: string;
  /** 재고 0 등으로 선택할 수 없는 사이즈. 비활성 표시되고 클릭이 무시된다. */
  disabledSizes?: string[];
  onSelect?: (size: string) => void;
}

export function SizeSelector({ sizes, selected, disabledSizes = [], onSelect }: SizeSelectorProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {sizes.map((size) => {
        const isDisabled = disabledSizes.includes(size);
        const isSelected = !isDisabled && size === selected;
        return (
          <button
            key={size}
            type="button"
            disabled={isDisabled}
            onClick={() => onSelect?.(size)}
            className={`text-title-sm h-11 min-w-11 rounded-[10px] px-1 ${
              isDisabled
                ? "border-[1.5px] border-hairline-soft bg-surface-soft text-muted-soft line-through"
                : isSelected
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
