"use client";

import { useState } from "react";

interface ProductImageProps {
  url: string;
  name: string;
}

export function ProductImage({ url, name }: ProductImageProps) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <div className="bg-surface-strong text-muted flex aspect-square items-center justify-center rounded-2xl p-2 text-center text-sm">
        {name}
      </div>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={name}
      className="aspect-square w-full rounded-2xl object-cover"
      onError={() => setFailed(true)}
    />
  );
}
