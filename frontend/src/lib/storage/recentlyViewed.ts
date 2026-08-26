import { readJSON, writeJSON } from "./safeStorage";

const KEY = "momentive:recently-viewed";
const MAX = 8;

export function getRecentlyViewed(): number[] {
  return readJSON<number[]>(KEY, []);
}

export function recordRecentlyViewed(productId: number): number[] {
  const current = getRecentlyViewed().filter((id) => id !== productId);
  const next = [productId, ...current].slice(0, MAX);
  writeJSON(KEY, next);
  return next;
}
