package com.momentive.backend.review.dto;

import com.momentive.backend.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;

public record MyReviewResponse(
        @Schema(description = "리뷰 ID") Long reviewId,
        @Schema(description = "별점(1~5)") Integer rating,
        @Schema(description = "리뷰 내용") String text
) {

    public static MyReviewResponse from(Review review) {
        return new MyReviewResponse(review.getId(), review.getRating(), review.getText());
    }
}
