/** Small status pill floated over product photography or inline with a title. */
export interface BadgeProps {
  label: string;
  /** @default 'new' */
  tone?: 'new' | 'sale' | 'soldout' | 'neutral';
}
