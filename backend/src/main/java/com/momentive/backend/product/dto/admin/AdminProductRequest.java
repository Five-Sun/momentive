package com.momentive.backend.product.dto.admin;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 관리자 상품 등록/수정 요청. 이미지와 variant는 부분 갱신이 아니라 전체 교체다.
 *
 * <p>형식 검증만 Bean Validation으로 처리하고, DB 조회가 필요한 규칙
 * (variant 0개, 사이즈 중복, 주문에 사용된 variant 삭제, 이미지 장수 초과)은
 * Service에서 {@code CustomException}으로 처리한다.
 */
public record AdminProductRequest(
        @Schema(description = "상품명") @NotBlank @Size(max = 200) String name,

        @Schema(description = "상품 설명") @NotBlank String description,

        @Schema(description = "정가") @NotNull @Min(0) Integer price,

        @Schema(description = "할인가. 없으면 null") @Min(0) Integer discountPrice,

        @Schema(description = "카테고리") @NotNull Category category,

        @Schema(description = "판매 상태. 생략하면 ON_SALE") ProductStatus status,

        @Schema(description = "이미지 URL 목록(최대 5장). 배열 순서가 그대로 displayOrder가 된다. 0장 허용")
        List<String> imageUrls,

        @Schema(description = "사이즈별 재고 목록. 최소 1개 필요하며, 사이즈가 없는 상품은 size가 null인 단일 항목")
        List<@Valid AdminProductVariantRequest> variants
) {

    /**
     * 상태를 생략한 요청은 판매중으로 간주한다(신규 등록 시 기본값).
     */
    public ProductStatus statusOrDefault() {
        return status == null ? ProductStatus.ON_SALE : status;
    }
}
