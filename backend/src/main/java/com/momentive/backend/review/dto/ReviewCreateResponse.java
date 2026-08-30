package com.momentive.backend.review.dto;

import com.momentive.backend.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewCreateResponse(
        @Schema(description = "리뷰 ID") Long reviewId,
        @Schema(description = "별점(1~5)") Integer rating,
        @Schema(description = "리뷰 내용") String text,
        @Schema(description = "작성 일시") LocalDateTime createdAt
) {

    public static ReviewCreateResponse from(Review review) {
        return new ReviewCreateResponse(review.getId(), review.getRating(), review.getText(), review.getCreatedAt());
    }
}
