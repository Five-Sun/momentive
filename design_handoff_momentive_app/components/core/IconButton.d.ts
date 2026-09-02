/** Circular icon-only control — wishlist heart, back arrow, cart, filter toggle. */
export interface IconButtonProps {
  children: React.ReactNode;
  /** Toggled/selected state — e.g. a favorited product heart. Renders in brand pink. */
  active?: boolean;
  /** @default 40 */
  size?: number;
  /** @default 'outline' */
  variant?: 'outline' | 'filled';
  onClick?: () => void;
}
