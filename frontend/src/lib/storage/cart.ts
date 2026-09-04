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

/**
 * 라인아이템 키를 만드는 유일한 지점. 주문 응답(OrderItemResponse)처럼 장바구니 밖에서 온
 * 데이터로도 같은 키를 재구성할 수 있어야 하므로, 포맷을 여기 한 곳에만 둔다.
 */
export function cartKeyOf(productId: number, size: string | null): string {
  return `${productId}-${size ?? ""}`;
}

export function getCart(): CartItem[] {
  return readJSON<CartItem[]>(KEY, []);
}

export function addToCart(item: Omit<CartItem, "key" | "qty">, qty = 1): CartItem[] {
  const key = cartKeyOf(item.id, item.size);
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

/**
 * 여러 항목을 한 번의 쓰기로 제거한다. 결제가 확정된 뒤 그 주문에 포함된 항목만 걷어내는 데 쓴다
 * (부분결제를 지원하므로 장바구니 전체를 비우면 안 된다).
 */
export function removeCartItems(keys: string[]): CartItem[] {
  if (keys.length === 0) return getCart();
  const removing = new Set(keys);
  const next = getCart().filter((c) => !removing.has(c.key));
  writeJSON(KEY, next);
  return next;
}

export function getCartCount(): number {
  return getCart().reduce((sum, c) => sum + c.qty, 0);
}
