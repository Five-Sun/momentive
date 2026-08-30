package com.momentive.backend.address.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @Schema(description = "수령인") @NotBlank String recipient,
        @Schema(description = "연락처") @NotBlank String phone,
        @Schema(description = "우편번호") @NotBlank String zipcode,
        @Schema(description = "기본 주소") @NotBlank String address1,
        @Schema(description = "상세 주소") String address2,
        @Schema(description = "기본 배송지 여부") @NotNull Boolean isDefault
) {
}
