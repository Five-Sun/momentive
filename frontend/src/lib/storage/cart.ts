import { readJSON, writeJSON } from "./safeStorage";

const KEY = "momentive:cart";

export interface CartItem {
  /** `variant-${variantId}` — 재고 단위(사이즈)별로 라인아이템을 구분하기 위한 키 */
  key: string;
  /** 상품 ID */
  id: number;
  /** 재고 단위 ID. 주문 생성(`POST /orders`)이 이 값을 그대로 실어 보낸다 */
  variantId: number;
  title: string;
  /** 표시용 사이즈 스냅샷. 사이즈가 없는 상품은 null */
  size: string | null;
  unitPrice: number;
  qty: number;
}

/**
 * 라인아이템 키를 만드는 유일한 지점. 재고 단위(variant)가 곧 라인아이템이므로 `variantId`
 * 하나로 키가 결정된다. 포맷을 여기 한 곳에만 둔다.
 */
export function cartKeyOf(variantId: number): string {
  return `variant-${variantId}`;
}

/**
 * `variantId`가 없는 구 형식 항목을 걸러낸다. 그런 항목은 어느 재고 단위인지 알 수 없어
 * 주문 생성 시 재고 검증이 불가능하므로, 유령 항목으로 남기지 않고 조용히 버린다.
 */
function isCartItem(value: unknown): value is CartItem {
  if (typeof value !== "object" || value === null) return false;
  const item = value as Record<string, unknown>;
  return (
    typeof item.key === "string" &&
    typeof item.id === "number" &&
    typeof item.variantId === "number" &&
    typeof item.title === "string" &&
    (typeof item.size === "string" || item.size === null) &&
    typeof item.unitPrice === "number" &&
    typeof item.qty === "number"
  );
}

export function getCart(): CartItem[] {
  const raw = readJSON<unknown[]>(KEY, []);
  if (!Array.isArray(raw)) return [];
  const items = raw.filter(isCartItem);
  // 버려진 항목이 있으면 저장소에도 반영해, 매번 다시 걸러내지 않도록 한다.
  if (items.length !== raw.length) writeJSON(KEY, items);
  return items;
}

export function addToCart(item: Omit<CartItem, "key" | "qty">, qty = 1): CartItem[] {
  const key = cartKeyOf(item.variantId);
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
