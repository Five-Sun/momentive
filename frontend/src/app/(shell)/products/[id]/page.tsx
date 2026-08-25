import { notFound } from "next/navigation";
import { Badge } from "@/components/core/Badge";
import { ProductImage } from "@/components/commerce/ProductImage";
import { getProduct } from "@/lib/api/products";

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const productId = Number(id);
  if (!Number.isInteger(productId)) notFound();

  const product = await getProduct(productId);
  if (!product) notFound();

  const hasDiscount = !product.soldOut && product.discountPrice != null;

  return (
    <main className="bg-canvas flex min-h-screen flex-col gap-6 p-4 pb-16">
      <div className="flex flex-col gap-3">
        {product.images.length === 0 ? (
          <div className="bg-surface-strong text-muted flex aspect-square items-center justify-center rounded-2xl text-center text-sm">
            {product.name}
          </div>
        ) : (
          product.images.map((image) => (
            <ProductImage key={image.id} url={image.url} name={product.name} />
          ))
        )}
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex items-center gap-2">
          {product.soldOut && <Badge tone="soldout" label="품절" />}
          {hasDiscount && (
            <Badge
              tone="sale"
              label={`${Math.round((1 - product.discountPrice! / product.price) * 100)}%`}
            />
          )}
        </div>
        <h1 className="text-title text-ink">{product.name}</h1>
        <div className="flex items-baseline gap-1.5">
          {hasDiscount && (
            <span className="text-body-sm text-muted-soft line-through">
              {formatWon(product.price)}
            </span>
          )}
          <span className="text-price text-ink">
            {formatWon(hasDiscount ? product.discountPrice! : product.price)}
          </span>
        </div>
        <p className="text-body text-ink">{product.description}</p>
      </div>
    </main>
  );
}
