package com.momentive.backend.review.controller;

import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.auth.security.OptionalCurrentUser;
import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.review.dto.MyReviewResponse;
import com.momentive.backend.review.dto.ReviewCreateRequest;
import com.momentive.backend.review.dto.ReviewCreateResponse;
import com.momentive.backend.review.dto.ReviewListResponse;
import com.momentive.backend.review.dto.ReviewUpdateResponse;
import com.momentive.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "상품 리뷰 목록 조회(최신순, 비로그인도 조회 가능)")
    @GetMapping
    public ReviewListResponse getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @Parameter(hidden = true) @OptionalCurrentUser Long currentUserId) {
        return reviewService.getReviews(productId, page, size, currentUserId);
    }

    @Operation(summary = "내가 이 상품에 쓴 리뷰 조회 (리뷰 없으면 JSON null 반환)")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
    @GetMapping("/me")
    public ResponseEntity<MyReviewResponse> getMyReview(
            @PathVariable Long productId, @Parameter(hidden = true) @CurrentUser Long userId) {
        // 리뷰가 없으면 reviewService.getMyReview()가 null을 반환한다. Spring MVC가 컨트롤러 반환값을
        // null로 그대로 응답하면 바디 자체가 생략되어 200 + 빈 바디(Content-Length: 0)가 나가므로,
        // ResponseEntity.ok(null)로 명시해 Jackson이 JSON literal null을 실제로 직렬화하도록 강제한다.
        return ResponseEntity.ok(reviewService.getMyReview(productId, userId));
    }

    @Operation(summary = "리뷰 작성")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCreateResponse createReview(
            @PathVariable Long productId,
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return reviewService.createReview(productId, userId, request);
    }

    @Operation(summary = "리뷰 수정")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
    @PatchMapping("/{reviewId}")
    public ReviewUpdateResponse updateReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return reviewService.updateReview(productId, reviewId, userId, request);
    }

    @Operation(summary = "리뷰 삭제")
    @SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Parameter(hidden = true) @CurrentUser Long userId) {
        reviewService.deleteReview(productId, reviewId, userId);
    }
}
