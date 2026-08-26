import { readJSON, writeJSON } from "./safeStorage";

const KEY = "momentive:wishlist";

export function getWishlist(): number[] {
  return readJSON<number[]>(KEY, []);
}

export function isWishlisted(productId: number): boolean {
  return getWishlist().includes(productId);
}

export function toggleWishlist(productId: number): number[] {
  const current = getWishlist();
  const next = current.includes(productId)
    ? current.filter((id) => id !== productId)
    : [...current, productId];
  writeJSON(KEY, next);
  return next;
}
