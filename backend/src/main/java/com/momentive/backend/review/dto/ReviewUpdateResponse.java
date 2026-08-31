package com.momentive.backend.review.dto;

import com.momentive.backend.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewUpdateResponse(
        @Schema(description = "리뷰 ID") Long reviewId,
        @Schema(description = "별점(1~5)") Integer rating,
        @Schema(description = "리뷰 내용") String text,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {

    public static ReviewUpdateResponse from(Review review) {
        return new ReviewUpdateResponse(review.getId(), review.getRating(), review.getText(), review.getUpdatedAt());
    }
}
