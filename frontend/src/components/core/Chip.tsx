interface ChipProps {
  label: string;
  selected?: boolean;
  onClick?: () => void;
}

export function Chip({ label, selected = false, onClick }: ChipProps) {
  return (
    <button
      onClick={onClick}
      className={`text-caption h-9 whitespace-nowrap rounded-full border-[1.5px] px-4 ${
        selected ? "animate-paw-pop" : ""
      } ${
        selected
          ? "border-ink bg-ink text-white"
          : "border-hairline bg-surface-card text-ink"
      }`}
    >
      {label}
    </button>
  );
}
