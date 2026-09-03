/** Primary interactive control for the Momentive app — pill-shaped, brand-pink fill. */
export interface ButtonProps {
  /** @default 'primary' */
  variant?: 'primary' | 'secondary' | 'ghost';
  /** @default 'md' */
  size?: 'md' | 'sm';
  disabled?: boolean;
  icon?: React.ReactNode;
  children: React.ReactNode;
  onClick?: () => void;
}
