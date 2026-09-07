import { GlobalBottomNav } from "@/components/navigation/GlobalBottomNav";
import { TopNav } from "@/components/navigation/TopNav";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import { fetchServerUser } from "@/lib/auth/serverUser";

export default async function ShellLayout({ children }: { children: React.ReactNode }) {
  const initialUser = await fetchServerUser();

  return (
    <AuthProvider initialUser={initialUser}>
      <div className="bg-canvas flex min-h-screen flex-col">
        <TopNav />
        <div className="mx-auto flex w-full max-w-[480px] flex-1 flex-col lg:max-w-[1400px] lg:px-10">
          <div className="flex-1">{children}</div>
          <div className="sticky bottom-0">
            <GlobalBottomNav />
          </div>
        </div>
      </div>
    </AuthProvider>
  );
}
