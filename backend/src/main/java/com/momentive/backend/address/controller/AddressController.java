package com.momentive.backend.address.controller;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.address.service.AddressService;
import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "배송지 목록 조회")
    @GetMapping
    public List<AddressResponse> getAddresses(@Parameter(hidden = true) @CurrentUser Long userId) {
        return addressService.getAddresses(userId);
    }

    @Operation(summary = "배송지 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(
            @Parameter(hidden = true) @CurrentUser Long userId, @Valid @RequestBody AddressRequest request) {
        return addressService.createAddress(userId, request);
    }

    @Operation(summary = "배송지 수정")
    @PatchMapping("/{addressId}")
    public AddressResponse updateAddress(@Parameter(hidden = true) @CurrentUser Long userId,
                                          @PathVariable Long addressId,
                                          @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(userId, addressId, request);
    }
}
