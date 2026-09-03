/** Bottom sheet for sort/filter — search results, category listing. */
export interface FilterSheetProps {
  open: boolean;
  sortOptions: string[];
  selected: string;
  onSelect: (option: string) => void;
  onApply: () => void;
  onClose: () => void;
}
