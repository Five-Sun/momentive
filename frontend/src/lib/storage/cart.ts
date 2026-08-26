import { readJSON, writeJSON } from "./safeStorage";

const KEY = "momentive:cart";

export interface CartItem {
  /** `${productId}-${size}` — 사이즈별로 별도 라인아이템을 구분하기 위한 키 */
  key: string;
  id: number;
  title: string;
  size: string;
  unitPrice: number;
  qty: number;
}

export function getCart(): CartItem[] {
  return readJSON<CartItem[]>(KEY, []);
}

export function addToCart(item: Omit<CartItem, "key" | "qty">, qty = 1): CartItem[] {
  const key = `${item.id}-${item.size}`;
  const current = getCart();
  const existing = current.find((c) => c.key === key);
  const next = existing
    ? current.map((c) => (c.key === key ? { ...c, qty: c.qty + qty } : c))
    : [...current, { ...item, key, qty }];
  writeJSON(KEY, next);
  return next;
}

export function updateCartQty(key: string, qty: number): CartItem[] {
  const current = getCart();
  const next =
    qty <= 0 ? current.filter((c) => c.key !== key) : current.map((c) => (c.key === key ? { ...c, qty } : c));
  writeJSON(KEY, next);
  return next;
}

export function removeFromCart(key: string): CartItem[] {
  const next = getCart().filter((c) => c.key !== key);
  writeJSON(KEY, next);
  return next;
}

export function getCartCount(): number {
  return getCart().reduce((sum, c) => sum + c.qty, 0);
}
