/**
 * 배송 관리 폼의 택배사 드롭다운 선택지. "기타" 선택 시 자유 텍스트 입력이 노출된다
 * (spec `docs/specs/2026-09-07-order-shipping-management.md` 인터페이스 > 화면).
 */
export const COURIER_OPTIONS = [
  "CJ대한통운",
  "우체국택배",
  "한진택배",
  "로젠택배",
  "롯데택배",
] as const;

export const COURIER_OTHER = "기타";
