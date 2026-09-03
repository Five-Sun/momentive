/** Free-shipping threshold nudge — cart screen, above the item list. */
export interface ShippingProgressProps {
  /** Amount (KRW) still needed to reach free shipping; 0 or less means qualified. */
  remaining: number;
  formatAmount: (n: number) => string;
}
