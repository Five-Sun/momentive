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
  averageRating: number | null;
  reviewCount: number;
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

/** 재고 단위. 사이즈가 없는 상품은 `size`가 null인 단일 항목으로 내려온다. */
export interface ProductVariant {
  variantId: number;
  size: string | null;
  stock: number;
  soldOut: boolean;
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
  variants: ProductVariant[];
  averageRating: number | null;
  reviewCount: number;
}

export interface GetProductsOptions {
  category?: Category;
  sort?: ProductSort;
  /** 상품명 부분일치 검색어. 서버가 필터링하므로 클라이언트에서 다시 거르지 않는다. */
  q?: string;
}

export async function getProducts(
  page = 0,
  size = 20,
  { category, sort, q }: GetProductsOptions = {}
): Promise<ProductListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (category) params.set("category", category);
  if (sort) params.set("sort", sort);
  if (q) params.set("q", q);

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
