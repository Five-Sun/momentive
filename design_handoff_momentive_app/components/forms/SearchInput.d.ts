/** Pill search field — homepage header and search screen. */
export interface SearchInputProps {
  value: string;
  onChange?: (value: string) => void;
  /** @default '검색어를 입력하세요' */
  placeholder?: string;
}
