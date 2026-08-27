import { notFound } from "next/navigation";
import { ProductDetailView } from "@/components/commerce/ProductDetailView";
import { getProduct } from "@/lib/api/products";

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

  return <ProductDetailView product={product} />;
}
