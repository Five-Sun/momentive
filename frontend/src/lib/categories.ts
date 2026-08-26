import type { Category } from "@/lib/api/products";

export const CATEGORY_LIST: { key: Category; label: string; description: string }[] = [
  { key: "OUTER", label: "아우터", description: "패딩 · 코트 · 후드집업" },
  { key: "KNIT", label: "니트", description: "스웨터 · 가디건" },
  { key: "INNERWEAR", label: "이너웨어", description: "티셔츠 · 레이어드" },
  { key: "ACCESSORY", label: "악세서리", description: "비니 · 스카프" },
];

export const CATEGORY_KEYS = CATEGORY_LIST.map((c) => c.key);

export function isCategory(value: string | null): value is Category {
  return value != null && (CATEGORY_KEYS as string[]).includes(value);
}
