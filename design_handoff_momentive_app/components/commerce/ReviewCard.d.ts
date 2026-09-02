/** Compact customer review row — product detail review section. */
export interface ReviewCardProps {
  author: string;
  rating: number;
  date: string;
  text: string;
  /** Number of photo placeholders to show. @default 0 */
  photoCount?: number;
}
