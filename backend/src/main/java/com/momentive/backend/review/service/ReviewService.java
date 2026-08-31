package com.momentive.backend.review.service;

import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.repository.OrderItemRepository;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.review.domain.Review;
import com.momentive.backend.review.dto.MyReviewResponse;
import com.momentive.backend.review.dto.ReviewCreateRequest;
import com.momentive.backend.review.dto.ReviewCreateResponse;
import com.momentive.backend.review.dto.ReviewListResponse;
import com.momentive.backend.review.dto.ReviewResponse;
import com.momentive.backend.review.dto.ReviewUpdateResponse;
import com.momentive.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewListResponse getReviews(Long productId, int page, int size, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ReviewListResponse.from(reviews.map(review ->
                ReviewResponse.of(review, currentUserId != null && review.isOwnedBy(currentUserId))));
    }

    public MyReviewResponse getMyReview(Long productId, Long userId) {
        verifyPurchase(userId, productId);
        return reviewRepository.findByProductIdAndUserId(productId, userId)
                .map(MyReviewResponse::from)
                .orElse(null);
    }

    @Transactional
    public ReviewCreateResponse createReview(Long productId, Long userId, ReviewCreateRequest request) {
        verifyPurchase(userId, productId);
        if (reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Product product = getProduct(productId);
        User user = getUser(userId);
        Review review = Review.create(product, user, request.rating(), request.text());
        reviewRepository.save(review);
        refreshRatingSummary(product);
        return ReviewCreateResponse.from(review);
    }

    @Transactional
    public ReviewUpdateResponse updateReview(Long productId, Long reviewId, Long userId, ReviewCreateRequest request) {
        Review review = getOwnedReview(productId, reviewId, userId);
        review.update(request.rating(), request.text());
        refreshRatingSummary(review.getProduct());
        return ReviewUpdateResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long productId, Long reviewId, Long userId) {
        Review review = getOwnedReview(productId, reviewId, userId);
        Product product = review.getProduct();
        reviewRepository.delete(review);
        refreshRatingSummary(product);
    }

    private void verifyPurchase(Long userId, Long productId) {
        boolean purchased = orderItemRepository.existsByOrder_User_IdAndOrder_StatusAndProduct_Id(
                userId, OrderStatus.PAID, productId);
        if (!purchased) {
            throw new CustomException(ErrorCode.PURCHASE_NOT_VERIFIED);
        }
    }

    private Review getOwnedReview(Long productId, Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        if (!review.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return review;
    }

    private void refreshRatingSummary(Product product) {
        long reviewCount = reviewRepository.countByProductId(product.getId());
        Double averageRating = reviewCount == 0 ? null : reviewRepository.findAverageRatingByProductId(product.getId());
        product.updateRatingSummary(averageRating, (int) reviewCount);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
