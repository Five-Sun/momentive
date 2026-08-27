import { Search } from "lucide-react";

interface SearchInputProps {
  value: string;
  onChange?: (value: string) => void;
  placeholder?: string;
}

export function SearchInput({
  value,
  onChange,
  placeholder = "검색어를 입력하세요",
}: SearchInputProps) {
  return (
    <div className="bg-surface-soft border-hairline flex h-12 items-center gap-2 rounded-full border px-[18px]">
      <Search className="text-muted h-4 w-4" />
      <input
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        placeholder={placeholder}
        className="text-body text-ink flex-1 border-none bg-transparent outline-none placeholder:text-muted"
      />
    </div>
  );
}
