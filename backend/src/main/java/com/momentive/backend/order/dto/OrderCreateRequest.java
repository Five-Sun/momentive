package com.momentive.backend.order.dto;

import com.momentive.backend.address.dto.AddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderCreateRequest(
        @Schema(description = "주문 상품 목록") @NotEmpty @Valid List<OrderItemRequest> items,
        @Schema(description = "저장된 배송지 ID (신규 배송지 입력 시 생략)") Long addressId,
        @Schema(description = "신규 배송지 정보 (저장된 배송지 사용 시 생략)") @Valid AddressRequest address
) {
}
