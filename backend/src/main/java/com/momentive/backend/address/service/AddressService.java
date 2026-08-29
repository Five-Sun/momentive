package com.momentive.backend.address.service;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = getUser(userId);
        Address address = createAddressEntity(user, request);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = getOwnedAddress(userId, addressId);
        address.update(request.recipient(), request.phone(), request.zipcode(), request.address1(), request.address2());
        if (Boolean.TRUE.equals(request.isDefault())) {
            applyAsDefault(userId, address);
        } else {
            address.unmarkAsDefault();
        }
        return AddressResponse.from(address);
    }

    /**
     * 신규 주소를 저장한다. 주문서 제출 시 배송지를 새로 입력하는 흐름(OrderService)에서도 재사용된다.
     */
    @Transactional
    public Address createAddressEntity(User user, AddressRequest request) {
        Address address = Address.create(
                user,
                request.recipient(),
                request.phone(),
                request.zipcode(),
                request.address1(),
                request.address2(),
                false
        );
        addressRepository.save(address);
        if (Boolean.TRUE.equals(request.isDefault())) {
            applyAsDefault(user.getId(), address);
        }
        return address;
    }

    private void applyAsDefault(Long userId, Address address) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .filter(existing -> !existing.getId().equals(address.getId()))
                .ifPresent(Address::unmarkAsDefault);
        address.markAsDefault();
    }

    private Address getOwnedAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
        if (!address.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return address;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
