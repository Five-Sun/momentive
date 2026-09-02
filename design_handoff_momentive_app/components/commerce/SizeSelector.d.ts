/** Dog-apparel size picker — S/M/L/XL cells, sized to breed rather than human clothing sizes. */
export interface SizeSelectorProps {
  sizes: string[];
  selected?: string;
  onSelect?: (size: string) => void;
}
