package com.momentive.backend.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.dto.AddressResponse;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.address.service.AddressService;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        addressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이"));
    }

    @Test
    void createAddress_sets_default_when_first_address() {
        User user = createUser("addr1@momentive.com");

        AddressResponse response = addressService.createAddress(user.getId(),
                new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true));

        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void createAddress_unmarks_previous_default_when_new_default_added() {
        User user = createUser("addr2@momentive.com");
        AddressResponse first = addressService.createAddress(user.getId(),
                new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", null, true));

        AddressResponse second = addressService.createAddress(user.getId(),
                new AddressRequest("몽이2", "010-3333-4444", "54321", "서울시 서초구", null, true));

        List<AddressResponse> addresses = addressService.getAddresses(user.getId());
        assertThat(addresses).filteredOn(a -> a.id().equals(first.id())).extracting(AddressResponse::isDefault).containsExactly(false);
        assertThat(addresses).filteredOn(a -> a.id().equals(second.id())).extracting(AddressResponse::isDefault).containsExactly(true);
    }

    @Test
    void updateAddress_fails_for_other_users_address() {
        User owner = createUser("owner@momentive.com");
        User other = createUser("other@momentive.com");
        AddressResponse address = addressService.createAddress(owner.getId(),
                new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", null, true));

        assertThatThrownBy(() -> addressService.updateAddress(other.getId(), address.id(),
                new AddressRequest("해커", "010-0000-0000", "00000", "다른 주소", null, false)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
