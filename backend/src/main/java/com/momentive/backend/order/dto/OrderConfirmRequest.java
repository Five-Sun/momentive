package com.momentive.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderConfirmRequest(
        @Schema(description = "토스페이먼츠 결제 키") @NotBlank String paymentKey,
        @Schema(description = "토스페이먼츠 주문 ID") @NotBlank String orderId,
        @Schema(description = "결제 금액") @NotNull @Positive Integer amount
) {
}
