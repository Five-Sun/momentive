package com.momentive.backend.address.dto;

import com.momentive.backend.address.domain.Address;

public record AddressResponse(
        Long id,
        String recipient,
        String phone,
        String zipcode,
        String address1,
        String address2,
        Boolean isDefault
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
