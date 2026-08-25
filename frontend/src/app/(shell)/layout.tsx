import { GlobalBottomNav } from "@/components/navigation/GlobalBottomNav";

export default function ShellLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-surface-strong flex min-h-screen justify-center">
      <div className="bg-canvas shadow-float flex w-full max-w-[480px] flex-col">
        <div className="flex-1">{children}</div>
        <div className="sticky bottom-0">
          <GlobalBottomNav />
        </div>
      </div>
    </div>
  );
}
