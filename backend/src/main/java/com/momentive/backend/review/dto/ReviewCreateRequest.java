package com.momentive.backend.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @Schema(description = "별점(1~5 정수)") @NotNull @Min(1) @Max(5) Integer rating,
        @Schema(description = "리뷰 내용(10~500자)") @NotNull @Size(min = 10, max = 500) String text
) {
}
