/** Photo-first product tile — the core repeating unit across Home, Search, and Wishlist grids. */
export interface ProductCardProps {
  /** Product photo element (image-slot, <img>, or placeholder node). */
  image: React.ReactNode;
  title: string;
  /** Pre-formatted price string, e.g. "38,000원". */
  price: string;
  originalPrice?: string;
  badge?: React.ReactNode;
  favorited?: boolean;
  onToggleFavorite?: () => void;
  rating?: React.ReactNode;
}
