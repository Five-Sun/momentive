const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

export type Category = "OUTER" | "KNIT" | "INNERWEAR" | "ACCESSORY";

export type ProductSort = "new" | "popular" | "price_asc" | "price_desc";

export interface ProductSummary {
  id: number;
  name: string;
  price: number;
  discountPrice: number | null;
  soldOut: boolean;
  category: Category;
  thumbnailUrl: string | null;
}

export interface ProductListResponse {
  content: ProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductImage {
  id: number;
  url: string;
  displayOrder: number;
}

export interface ProductDetail {
  id: number;
  name: string;
  description: string;
  price: number;
  discountPrice: number | null;
  soldOut: boolean;
  category: Category;
  images: ProductImage[];
}

export interface GetProductsOptions {
  category?: Category;
  sort?: ProductSort;
}

export async function getProducts(
  page = 0,
  size = 20,
  { category, sort }: GetProductsOptions = {}
): Promise<ProductListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (category) params.set("category", category);
  if (sort) params.set("sort", sort);

  const res = await fetch(`${API_BASE_URL}/products?${params.toString()}`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`상품 목록 조회 실패: ${res.status}`);
  return res.json();
}

export async function getProduct(id: number): Promise<ProductDetail | null> {
  const res = await fetch(`${API_BASE_URL}/products/${id}`, { cache: "no-store" });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`상품 상세 조회 실패: ${res.status}`);
  return res.json();
}
