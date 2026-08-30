package com.momentive.backend.address.dto;

import com.momentive.backend.address.domain.Address;
import io.swagger.v3.oas.annotations.media.Schema;

public record AddressResponse(
        @Schema(description = "배송지 ID") Long id,
        @Schema(description = "수령인") String recipient,
        @Schema(description = "연락처") String phone,
        @Schema(description = "우편번호") String zipcode,
        @Schema(description = "기본 주소") String address1,
        @Schema(description = "상세 주소") String address2,
        @Schema(description = "기본 배송지 여부") Boolean isDefault
) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getZipcode(),
                address.getAddress1(),
                address.getAddress2(),
                address.getIsDefault()
        );
    }
}
