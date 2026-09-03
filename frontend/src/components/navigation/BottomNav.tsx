import type { ReactNode } from "react";

interface NavItem {
  key: string;
  icon: ReactNode;
  label: string;
}

interface BottomNavProps {
  items: NavItem[];
  activeKey: string;
  onSelect?: (key: string) => void;
}

export function BottomNav({ items, activeKey, onSelect }: BottomNavProps) {
  return (
    <div className="bg-surface-card border-hairline flex h-16 items-center justify-around border-t">
      {items.map((item) => {
        const active = item.key === activeKey;
        return (
          <button
            key={item.key}
            onClick={() => onSelect?.(item.key)}
            className={`flex flex-col items-center gap-0.5 ${
              active ? "text-brand-pink-active" : "text-muted"
            }`}
          >
            <span key={active ? `${item.key}-active` : item.key} className={`text-xl ${active ? "animate-bump-up" : ""}`}>
              {item.icon}
            </span>
            <span className={`text-caption ${active ? "font-bold" : ""}`}>{item.label}</span>
          </button>
        );
      })}
    </div>
  );
}
