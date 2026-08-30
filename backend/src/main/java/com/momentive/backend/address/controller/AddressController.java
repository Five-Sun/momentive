package com.momentive.backend.address.controller;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.address.service.AddressService;
import com.momentive.backend.auth.security.CurrentUser;
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
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> getAddresses(@CurrentUser Long userId) {
        return addressService.getAddresses(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(@CurrentUser Long userId, @Valid @RequestBody AddressRequest request) {
        return addressService.createAddress(userId, request);
    }

    @PatchMapping("/{addressId}")
    public AddressResponse updateAddress(@CurrentUser Long userId, @PathVariable Long addressId,
                                          @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(userId, addressId, request);
    }
}
