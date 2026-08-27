import { readJSON, writeJSON } from "./safeStorage";

const KEY = "momentive:recent-searches";
const MAX = 10;

export function getRecentSearches(): string[] {
  return readJSON<string[]>(KEY, []);
}

export function recordRecentSearch(query: string): string[] {
  const trimmed = query.trim();
  if (!trimmed) return getRecentSearches();
  const current = getRecentSearches().filter((q) => q !== trimmed);
  const next = [trimmed, ...current].slice(0, MAX);
  writeJSON(KEY, next);
  return next;
}

export function clearRecentSearches(): void {
  writeJSON(KEY, []);
}
