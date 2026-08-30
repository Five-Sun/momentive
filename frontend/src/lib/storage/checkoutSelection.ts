import { readSessionJSON, writeSessionJSON } from "./safeStorage";

const KEY = "momentive:checkout-selection";

/**
 * 장바구니에서 "구매하기"를 누른 시점에 선택된 CartItem.key 목록.
 * 결제 전 잠깐 필요한 세션성 UI 상태이므로 sessionStorage에 둔다(localStorage의 cart 자체와는 무관).
 * `/checkout`(Phase 4)이 이 목록을 읽어 장바구니에서 해당 항목만 주문 대상으로 사용한다.
 */
export function setCheckoutSelection(keys: string[]): void {
  writeSessionJSON(KEY, keys);
}

export function getCheckoutSelection(): string[] {
  return readSessionJSON<string[]>(KEY, []);
}
