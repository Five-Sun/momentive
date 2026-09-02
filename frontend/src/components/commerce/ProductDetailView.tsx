"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Heart, Minus, Plus } from "lucide-react";
import { Badge } from "@/components/core/Badge";
import { Button } from "@/components/core/Button";
import { IconButton } from "@/components/core/IconButton";
import { Toast } from "@/components/feedback/Toast";
import { ProductImage } from "@/components/commerce/ProductImage";
import { Rating } from "@/components/commerce/Rating";
import { ReviewCard } from "@/components/commerce/ReviewCard";
import { ReviewForm, type ReviewFormValues } from "@/components/commerce/ReviewForm";
import { SizeSelector } from "@/components/commerce/SizeSelector";
import { getProduct, type ProductDetail } from "@/lib/api/products";
import {
  createReview,
  deleteReview,
  getMyReview,
  getReviews,
  updateReview,
  type MyReview,
  type Review,
} from "@/lib/api/reviews";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import { isWishlisted, toggleWishlist } from "@/lib/storage/wishlist";
import { addToCart } from "@/lib/storage/cart";
import { recordRecentlyViewed } from "@/lib/storage/recentlyViewed";

const SIZES = ["S", "M", "L", "XL"];

const SIZE_GUIDE: [string, string][] = [
  ["S", "3~5kg · 등길이 25cm"],
  ["M", "5~8kg · 등길이 30cm"],
  ["L", "8~12kg · 등길이 35cm"],
  ["XL", "12kg~ · 등길이 40cm"],
];

const DELIVERY_INFO = [
  "오후 2시 이전 결제 시 당일 출고",
  "제주/도서산간 지역 추가 배송비 3,000원",
  "단순 변심 교환/반품은 상품 수령 후 7일 이내 가능",
];

const REVIEW_PAGE_SIZE = 5;

function formatWon(amount: number) {
  return `${amount.toLocaleString("ko-KR")}원`;
}

export function ProductDetailView({ product }: { product: ProductDetail }) {
  const router = useRouter();
  const { user } = useAuth();
  const [favorited, setFavorited] = useState(false);
  const [size, setSize] = useState<string | null>(null);
  const [openPanel, setOpenPanel] = useState<"guide" | "delivery" | null>(null);
  const [toastVisible, setToastVisible] = useState(false);
  const [toastMessage, setToastMessage] = useState("장바구니에 담았어요");

  const [averageRating, setAverageRating] = useState(product.averageRating);
  const [reviewCount, setReviewCount] = useState(product.reviewCount);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewPage, setReviewPage] = useState(0);
  const [hasNextReviewPage, setHasNextReviewPage] = useState(false);
  const [reviewsLoading, setReviewsLoading] = useState(false);

  const [myReview, setMyReview] = useState<MyReview | null>(null);
  const [purchaseVerified, setPurchaseVerified] = useState(false);
  const [myReviewChecked, setMyReviewChecked] = useState(false);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewSubmitting, setReviewSubmitting] = useState(false);

  useEffect(() => {
    recordRecentlyViewed(product.id);
    Promise.resolve().then(() => setFavorited(isWishlisted(product.id)));
  }, [product.id]);

  const loadReviews = useCallback((page: number) => {
    setReviewsLoading(true);
    getReviews(product.id, page, REVIEW_PAGE_SIZE)
      .then((res) => {
        setReviews((prev) => (page === 0 ? res.reviews : [...prev, ...res.reviews]));
        setHasNextReviewPage(res.hasNext);
        setReviewPage(page);
      })
      .finally(() => setReviewsLoading(false));
  }, [product.id]);

  const refreshRatingSummary = useCallback(async () => {
    const detail = await getProduct(product.id);
    if (detail) {
      setAverageRating(detail.averageRating);
      setReviewCount(detail.reviewCount);
    }
  }, [product.id]);

  useEffect(() => {
    Promise.resolve().then(() => loadReviews(0));
  }, [loadReviews]);

  useEffect(() => {
    if (!user) {
      Promise.resolve().then(() => {
        setPurchaseVerified(false);
        setMyReview(null);
        setMyReviewChecked(true);
      });
      return;
    }
    Promise.resolve().then(() => setMyReviewChecked(false));
    getMyReview(product.id)
      .then((review) => {
        setPurchaseVerified(true);
        setMyReview(review);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.errorCode === "PURCHASE_NOT_VERIFIED") {
          setPurchaseVerified(false);
          setMyReview(null);
          return;
        }
        // 예상 못한 에러(네트워크 오류, 응답 파싱 실패 등)를 구매 미확인으로 오인해
        // 조용히 묻으면 실제로는 구매한 사용자에게도 리뷰 작성 버튼이 사라지는 버그로
        // 이어질 수 있다. 콘솔에 남기고 사용자에게도 알려 원인 파악이 가능하게 한다.
        console.error("내 리뷰 조회 실패", err);
        setPurchaseVerified(false);
        setMyReview(null);
        showToast("리뷰 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
      })
      .finally(() => setMyReviewChecked(true));
  }, [product.id, user]);

  function showToast(message: string) {
    setToastMessage(message);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 1800);
  }

  async function handleReviewSubmit(values: ReviewFormValues) {
    setReviewSubmitting(true);
    try {
      const isUpdate = myReview != null;
      if (myReview) {
        const updated = await updateReview(product.id, myReview.reviewId, values);
        setMyReview({ reviewId: updated.reviewId, rating: updated.rating, text: updated.text });
      } else {
        const created = await createReview(product.id, values);
        setMyReview({ reviewId: created.reviewId, rating: created.rating, text: created.text });
      }
      setShowReviewForm(false);
      loadReviews(0);
      await refreshRatingSummary();
      showToast(isUpdate ? "리뷰를 수정했어요" : "리뷰를 등록했어요");
    } finally {
      setReviewSubmitting(false);
    }
  }

  async function handleReviewDelete(reviewId: number) {
    if (!window.confirm("리뷰를 삭제할까요?")) return;
    try {
      await deleteReview(product.id, reviewId);
      setMyReview(null);
      setShowReviewForm(false);
      loadReviews(0);
      await refreshRatingSummary();
      showToast("리뷰를 삭제했어요");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "리뷰 삭제에 실패했어요");
    }
  }

  const hasDiscount = !product.soldOut && product.discountPrice != null;
  const unitPrice = hasDiscount ? product.discountPrice! : product.price;

  function handleToggleWishlist() {
    setFavorited(toggleWishlist(product.id).includes(product.id));
  }

  function handleAddToCart() {
    if (!size || product.soldOut) return;
    addToCart({ id: product.id, title: product.name, size, unitPrice });
    showToast("장바구니에 담았어요");
  }

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center justify-between border-b px-4 lg:hidden">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <IconButton size={32} active={favorited} onClick={handleToggleWishlist}>
          <Heart className="h-4 w-4" fill={favorited ? "currentColor" : "none"} />
        </IconButton>
      </div>

      <div className="lg:grid lg:grid-cols-[1fr_1fr] lg:items-start lg:gap-10 lg:pt-8">
        <div className="flex flex-col gap-3 p-4 lg:sticky lg:top-8 lg:p-0">
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

        <div className="flex flex-col gap-3 px-4 lg:px-0">
          <button
            onClick={() => router.back()}
            className="text-caption text-muted hidden items-center lg:flex"
          >
            <ArrowLeft className="mr-1 h-3.5 w-3.5" />
            목록으로
          </button>
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
          {averageRating != null && <Rating value={averageRating} count={reviewCount} />}
          <div className="flex items-baseline gap-1.5">
            {hasDiscount && (
              <span className="text-body-sm text-muted-soft line-through">
                {formatWon(product.price)}
              </span>
            )}
            <span className="text-price text-ink">{formatWon(unitPrice)}</span>
          </div>

          <div className="bg-hairline my-1.5 h-px" />

          <div className="flex items-center justify-between">
            <span className="text-title-sm text-ink">사이즈</span>
            <button
              onClick={() => setOpenPanel(openPanel === "guide" ? null : "guide")}
              className="text-caption text-brand-pink-active underline"
            >
              사이즈 가이드
            </button>
          </div>
          <div className={product.soldOut ? "pointer-events-none opacity-50" : undefined}>
            <SizeSelector sizes={SIZES} selected={size ?? ""} onSelect={setSize} />
          </div>
          {openPanel === "guide" && (
            <div className="bg-surface-soft rounded-sm flex flex-col gap-1.5 p-3.5">
              <span className="text-caption text-ink font-bold">반려견 체형별 사이즈</span>
              {SIZE_GUIDE.map(([s, d]) => (
                <div key={s} className="text-body-sm text-body flex justify-between">
                  <span>{s}</span>
                  <span className="text-muted">{d}</span>
                </div>
              ))}
            </div>
          )}

          <div className="bg-hairline my-1.5 h-px" />

          <span className="text-title-sm text-ink">상품 설명</span>
          <p className="text-body text-ink m-0">{product.description}</p>

          <div className="hidden lg:mt-2 lg:flex lg:gap-2.5">
            <Button variant="secondary" onClick={handleToggleWishlist}>
              {favorited ? "위시 완료" : "위시 담기"}
            </Button>
            <div className="flex-1">
              <Button
                variant="primary"
                disabled={!size || product.soldOut}
                onClick={handleAddToCart}
              >
                장바구니 담기
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3 px-4 pt-6 lg:px-0 lg:pt-10">
        <button
          onClick={() => setOpenPanel(openPanel === "delivery" ? null : "delivery")}
          className="flex items-center justify-between"
        >
          <span className="text-title-sm text-ink">배송 · 교환/반품 안내</span>
          {openPanel === "delivery" ? (
            <Minus className="text-muted h-4 w-4" />
          ) : (
            <Plus className="text-muted h-4 w-4" />
          )}
        </button>
        {openPanel === "delivery" && (
          <div className="text-body-sm text-body flex flex-col gap-1">
            {DELIVERY_INFO.map((line) => (
              <span key={line}>· {line}</span>
            ))}
          </div>
        )}

        <div className="bg-hairline my-1.5 h-px" />

        <div className="flex items-center justify-between">
          <span className="text-title-sm text-ink">
            리뷰{reviewCount > 0 ? ` ${reviewCount}` : ""}
          </span>
          {myReviewChecked &&
            (purchaseVerified ? (
              <button
                onClick={() => setShowReviewForm((prev) => !prev)}
                className="text-caption text-brand-pink-active underline"
              >
                {showReviewForm ? "취소" : myReview ? "리뷰 수정" : "리뷰 쓰기"}
              </button>
            ) : (
              <span className="text-caption text-muted">
                {user ? "구매한 상품만 리뷰를 쓸 수 있어요" : "로그인 후 구매하면 리뷰를 쓸 수 있어요"}
              </span>
            ))}
        </div>

        {showReviewForm && purchaseVerified && (
          <ReviewForm
            initialValues={myReview ? { rating: myReview.rating, text: myReview.text } : undefined}
            submitting={reviewSubmitting}
            onCancel={() => setShowReviewForm(false)}
            onSubmit={handleReviewSubmit}
            onApiError={(err, setFieldError) => {
              if (err.errorCode === "VALIDATION_FAILED" && err.fieldErrors) {
                for (const [field, message] of Object.entries(err.fieldErrors)) {
                  if (field === "rating" || field === "text") {
                    setFieldError(field, message);
                  }
                }
                return;
              }
              if (err.errorCode === "REVIEW_ALREADY_EXISTS") {
                showToast("이미 작성한 리뷰가 있어요");
                return;
              }
              showToast(err.message || "리뷰 저장에 실패했어요. 잠시 후 다시 시도해주세요");
            }}
          />
        )}

        {reviews.length === 0 ? (
          <p className="text-body-sm text-muted m-0">아직 작성된 리뷰가 없어요.</p>
        ) : (
          <div className="flex flex-col gap-3">
            {reviews.map((review) => (
              <ReviewCard
                key={review.reviewId}
                authorNickname={review.authorNickname}
                rating={review.rating}
                createdAt={review.createdAt}
                text={review.text}
                isMine={review.isMine}
                onEdit={() => setShowReviewForm(true)}
                onDelete={() => handleReviewDelete(review.reviewId)}
              />
            ))}
          </div>
        )}

        {hasNextReviewPage && (
          <Button variant="secondary" onClick={() => loadReviews(reviewPage + 1)}>
            {reviewsLoading ? "불러오는 중..." : "더보기"}
          </Button>
        )}
      </div>

      <div className="border-hairline bg-surface-card sticky bottom-16 flex gap-2.5 border-t p-3.5 lg:hidden">
        <Button variant="secondary" onClick={handleToggleWishlist}>
          {favorited ? "위시 완료" : "위시 담기"}
        </Button>
        <div className="flex-1">
          <Button
            variant="primary"
            disabled={!size || product.soldOut}
            onClick={handleAddToCart}
          >
            장바구니 담기
          </Button>
        </div>
      </div>

      <Toast message={toastMessage} visible={toastVisible} />
    </div>
  );
}
