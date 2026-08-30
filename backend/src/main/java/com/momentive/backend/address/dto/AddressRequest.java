package com.momentive.backend.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotBlank String recipient,
        @NotBlank String phone,
        @NotBlank String zipcode,
        @NotBlank String address1,
        String address2,
        @NotNull Boolean isDefault
) {
}
