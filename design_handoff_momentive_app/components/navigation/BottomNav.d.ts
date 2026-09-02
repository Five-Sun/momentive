/** App-wide bottom tab bar. */
export interface BottomNavItem {
  key: string;
  icon: React.ReactNode;
  label: string;
}
export interface BottomNavProps {
  items: BottomNavItem[];
  activeKey: string;
  onSelect?: (key: string) => void;
}
