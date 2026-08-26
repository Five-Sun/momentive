export function ProductCardSkeleton() {
  return (
    <div className="flex w-full animate-pulse flex-col gap-2">
      <div className="bg-surface-strong aspect-square rounded-2xl" />
      <div className="flex flex-col gap-2">
        <div className="bg-surface-strong h-4 w-3/4 rounded-full" />
        <div className="bg-surface-strong h-3 w-1/3 rounded-full" />
        <div className="bg-surface-strong h-4 w-1/2 rounded-full" />
      </div>
    </div>
  );
}
