package com.momentive.backend.order.dto;

import com.momentive.backend.address.dto.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        Long addressId,
        @Valid AddressRequest address
) {
}
