package com.momentive.backend.pet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.pet.domain.PetGender;
import com.momentive.backend.pet.dto.PetListResponse;
import com.momentive.backend.pet.dto.PetRequest;
import com.momentive.backend.pet.dto.PetResponse;
import com.momentive.backend.pet.repository.PetRepository;
import com.momentive.backend.pet.service.PetService;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PetServiceTest {

    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        petRepository.deleteAll();
        userCouponRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이"));
    }

    private PetRequest fullRequest(String name) {
        return new PetRequest(name, "말티즈", LocalDate.of(2022, 3, 1), PetGender.FEMALE, 3.2);
    }

    private PetRequest nameOnlyRequest(String name) {
        return new PetRequest(name, null, null, null, null);
    }

    @Test
    void createPet_succeeds_with_name_only() {
        User user = createUser("pet1@momentive.com");

        PetResponse response = petService.createPet(user.getId(), nameOnlyRequest("몽이"));

        assertThat(response.name()).isEqualTo("몽이");
        assertThat(response.breed()).isNull();
        assertThat(response.birthDate()).isNull();
        assertThat(response.gender()).isNull();
        assertThat(response.weightKg()).isNull();
    }

    @Test
    void getPets_returns_only_own_pets_ordered_by_latest() throws InterruptedException {
        User owner = createUser("pet2@momentive.com");
        User other = createUser("pet2-other@momentive.com");
        petService.createPet(owner.getId(), fullRequest("첫째"));
        Thread.sleep(5);
        petService.createPet(owner.getId(), fullRequest("둘째"));
        petService.createPet(other.getId(), fullRequest("남의개"));

        PetListResponse response = petService.getPets(owner.getId());

        assertThat(response.pets()).hasSize(2);
        assertThat(response.pets().get(0).name()).isEqualTo("둘째");
        assertThat(response.pets().get(1).name()).isEqualTo("첫째");
    }

    @Test
    void updatePet_changes_fields_for_owner() {
        User user = createUser("pet3@momentive.com");
        PetResponse created = petService.createPet(user.getId(), nameOnlyRequest("몽이"));

        PetResponse updated = petService.updatePet(user.getId(), created.id(),
                new PetRequest("몽이2", "푸들", LocalDate.of(2023, 1, 1), PetGender.MALE, 4.5));

        assertThat(updated.name()).isEqualTo("몽이2");
        assertThat(updated.breed()).isEqualTo("푸들");
        assertThat(updated.birthDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(updated.gender()).isEqualTo(PetGender.MALE);
        assertThat(updated.weightKg()).isEqualTo(4.5);
    }

    @Test
    void updatePet_and_deletePet_fail_for_non_owner() {
        User owner = createUser("pet4-owner@momentive.com");
        User other = createUser("pet4-other@momentive.com");
        PetResponse pet = petService.createPet(owner.getId(), nameOnlyRequest("몽이"));

        assertThatThrownBy(() -> petService.updatePet(other.getId(), pet.id(), fullRequest("해커개")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> petService.deletePet(other.getId(), pet.id()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void updatePet_and_deletePet_fail_when_pet_not_found() {
        User user = createUser("pet5@momentive.com");

        assertThatThrownBy(() -> petService.updatePet(user.getId(), 999999L, fullRequest("없는개")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PET_NOT_FOUND);

        assertThatThrownBy(() -> petService.deletePet(user.getId(), 999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PET_NOT_FOUND);
    }

    @Test
    void deletePet_allows_registering_new_pet_afterwards() {
        User user = createUser("pet6@momentive.com");
        PetResponse pet = petService.createPet(user.getId(), nameOnlyRequest("몽이"));

        petService.deletePet(user.getId(), pet.id());

        assertThat(petService.getPets(user.getId()).pets()).isEmpty();

        PetResponse rewritten = petService.createPet(user.getId(), nameOnlyRequest("몽이2"));
        assertThat(rewritten.name()).isEqualTo("몽이2");
        assertThat(petService.getPets(user.getId()).pets()).hasSize(1);
    }
}
