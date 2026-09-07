import { apiFetch } from "./client";
import type { Category } from "./products";

/**
 * 관리자 API 클라이언트.
 *
 * 모든 호출은 공통 `apiFetch`를 거친다 — 401 자동 refresh와 `ApiError` 변환을 그대로 물려받기
 * 위함이며, 실패는 여기서 문자열로 뭉개지 않고 `ApiError`를 그대로 던져 화면이 `errorCode`/
 * `fieldErrors`로 분기할 수 있게 한다. 파일 바이트를 Cloudinary로 직접 올리는 경로만 백엔드를
 * 거치지 않으므로 `src/lib/upload/cloudinary.ts`에 따로 두었다.
 */

export type ProductStatus = "ON_SALE" | "HIDDEN" | "DELETED";

export interface AdminProductSummary {
  id: number;
  name: string;
  category: Category;
  price: number;
  discountPrice: number | null;
  totalStock: number;
  status: ProductStatus;
  thumbnailUrl: string | null;
}

export interface AdminProductListResponse {
  content: AdminProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminProductImage {
  id: number;
  url: string;
  displayOrder: number;
}

export interface AdminProductVariant {
  id: number;
  /** 사이즈 없는 상품은 null */
  size: string | null;
  stock: number;
}

export interface AdminProductDetail {
  id: number;
  name: string;
  description: string;
  price: number;
  discountPrice: number | null;
  category: Category;
  status: ProductStatus;
  images: AdminProductImage[];
  variants: AdminProductVariant[];
  totalStock: number;
  soldOut: boolean;
}

/**
 * 등록·수정 요청의 variant 한 행.
 *
 * `id`는 서버가 돌려준 값을 그대로 되돌려 보내야 한다. 값을 버리고 `null`로 보내면 서버가
 * "기존 행 삭제 + 새 행 INSERT"로 처리해, 이미 주문에 쓰인 사이즈는 `VARIANT_IN_USE`로 막힌다.
 */
export interface AdminProductVariantRequest {
  id: number | null;
  size: string | null;
  stock: number;
}

export interface AdminProductRequest {
  name: string;
  description: string;
  price: number;
  discountPrice: number | null;
  category: Category;
  status: ProductStatus;
  /** 배열 순서가 그대로 displayOrder가 된다. 최대 5장 */
  imageUrls: string[];
  variants: AdminProductVariantRequest[];
}

export interface ImageUploadSignature {
  signature: string;
  timestamp: number;
  apiKey: string;
  cloudName: string;
  folder: string;
}

export interface GetAdminProductsOptions {
  page?: number;
  size?: number;
  statuses?: ProductStatus[];
  q?: string;
}

export function getAdminProducts({
  page = 0,
  size = 20,
  statuses,
  q,
}: GetAdminProductsOptions = {}): Promise<AdminProductListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  // 서버는 status를 콤마로 이어붙인 복수 값으로 받는다(기본값 "ON_SALE,HIDDEN"과 같은 형식).
  if (statuses && statuses.length > 0) params.set("status", statuses.join(","));
  const keyword = q?.trim();
  if (keyword) params.set("q", keyword);

  return apiFetch<AdminProductListResponse>(`/admin/products?${params.toString()}`);
}

/** 관리자 상세 조회. DELETED 상품도 조회되고, 없는 상품은 `PRODUCT_NOT_FOUND` 404가 온다. */
export function getAdminProduct(id: number): Promise<AdminProductDetail> {
  return apiFetch<AdminProductDetail>(`/admin/products/${id}`);
}

export function createAdminProduct(request: AdminProductRequest): Promise<AdminProductDetail> {
  return apiFetch<AdminProductDetail>("/admin/products", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateAdminProduct(
  id: number,
  request: AdminProductRequest,
): Promise<AdminProductDetail> {
  return apiFetch<AdminProductDetail>(`/admin/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

/** soft delete. 행은 남고 status만 DELETED로 바뀌어 기존 주문 이력은 그대로 보인다. */
export function deleteAdminProduct(id: number): Promise<void> {
  return apiFetch<void>(`/admin/products/${id}`, { method: "DELETE" });
}

/** Cloudinary signed upload 서명 발급. 응답에 API secret은 포함되지 않는다. */
export function issueImageUploadSignature(): Promise<ImageUploadSignature> {
  return apiFetch<ImageUploadSignature>("/admin/images/signature", { method: "POST" });
}
