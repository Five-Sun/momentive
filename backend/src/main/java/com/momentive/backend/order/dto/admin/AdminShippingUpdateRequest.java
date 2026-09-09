package com.momentive.backend.order.dto.admin;

import com.momentive.backend.order.domain.ShippingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * courier/trackingNumber의 조건부 필수 규칙(SHIPPING/DELIVERED 전환 시에만 필요)은 Bean
 * Validation이 아니라 {@link com.momentive.backend.order.domain.Order#updateShipping}이 처리하므로
 * 여기서는 형식 검증(null 여부)만 둔다.
 */
public record AdminShippingUpdateRequest(
        @Schema(description = "변경할 배송상태") @NotNull ShippingStatus shippingStatus,
        @Schema(description = "택배사(SHIPPING/DELIVERED 전환 시 필수)") String courier,
        @Schema(description = "송장번호(SHIPPING/DELIVERED 전환 시 필수)") String trackingNumber
) {
}
