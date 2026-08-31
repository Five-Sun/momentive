package com.momentive.backend.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record ReviewListResponse(
        @Schema(description = "리뷰 목록") List<ReviewResponse> reviews,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext,
        @Schema(description = "전체 리뷰 개수") long totalCount
) {

    public static ReviewListResponse from(Page<ReviewResponse> page) {
        return new ReviewListResponse(page.getContent(), page.hasNext(), page.getTotalElements());
    }
}
