package com.momentive.backend.review.dto;

import com.momentive.backend.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ReviewResponse(
        @Schema(description = "리뷰 ID") Long reviewId,
        @Schema(description = "작성자 닉네임") String authorNickname,
        @Schema(description = "별점(1~5)") Integer rating,
        @Schema(description = "리뷰 내용") String text,
        @Schema(description = "작성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt,
        @Schema(description = "현재 로그인 사용자 본인 작성 여부") boolean isMine
) {

    public static ReviewResponse of(Review review, boolean isMine) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getNickname(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                isMine
        );
    }
}
