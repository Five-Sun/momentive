import { apiFetch } from "./client";

export interface Review {
  reviewId: number;
  authorNickname: string;
  rating: number;
  text: string;
  createdAt: string;
  updatedAt: string;
  isMine: boolean;
}

export interface ReviewListResponse {
  reviews: Review[];
  hasNext: boolean;
  totalCount: number;
}

export interface MyReview {
  reviewId: number;
  rating: number;
  text: string;
}

export interface ReviewRequest {
  rating: number;
  text: string;
}

export interface ReviewCreateResponse {
  reviewId: number;
  rating: number;
  text: string;
  createdAt: string;
}

export interface ReviewUpdateResponse {
  reviewId: number;
  rating: number;
  text: string;
  updatedAt: string;
}

export function getReviews(productId: number, page = 0, size = 5): Promise<ReviewListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return apiFetch<ReviewListResponse>(`/products/${productId}/reviews?${params.toString()}`);
}

export function getMyReview(productId: number): Promise<MyReview | null> {
  return apiFetch<MyReview | null>(`/products/${productId}/reviews/me`);
}

export function createReview(productId: number, request: ReviewRequest): Promise<ReviewCreateResponse> {
  return apiFetch<ReviewCreateResponse>(`/products/${productId}/reviews`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateReview(
  productId: number,
  reviewId: number,
  request: ReviewRequest,
): Promise<ReviewUpdateResponse> {
  return apiFetch<ReviewUpdateResponse>(`/products/${productId}/reviews/${reviewId}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function deleteReview(productId: number, reviewId: number): Promise<void> {
  return apiFetch<void>(`/products/${productId}/reviews/${reviewId}`, {
    method: "DELETE",
  });
}
