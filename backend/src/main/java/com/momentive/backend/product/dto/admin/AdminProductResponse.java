package com.momentive.backend.product.dto.admin;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import com.momentive.backend.product.dto.ProductImageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 관리자 상품 상세 응답. 없는 상품은 이 타입의 {@code null}이 아니라
 * {@code PRODUCT_NOT_FOUND} 404로 응답하므로, 이 응답이 나가는 경우 바디는 항상 채워져 있다.
 */
public record AdminProductResponse(
        @Schema(description = "상품 ID") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "상품 설명") String description,
        @Schema(description = "정가") Integer price,
        @Schema(description = "할인가(없으면 null)") Integer discountPrice,
        @Schema(description = "카테고리") Category category,
        @Schema(description = "판매 상태") ProductStatus status,
        @Schema(description = "이미지 목록(displayOrder 오름차순)") List<ProductImageResponse> images,
        @Schema(description = "사이즈별 재고 목록") List<AdminProductVariantResponse> variants,
        @Schema(description = "전체 재고 합") Integer totalStock,
        @Schema(description = "품절 여부(재고 합 0에서 파생)") Boolean soldOut
) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getCategory(),
                product.getStatus(),
                product.getImages().stream().map(ProductImageResponse::from).toList(),
                product.getVariants().stream().map(AdminProductVariantResponse::from).toList(),
                product.getTotalStock(),
                product.isSoldOut()
        );
    }
}
