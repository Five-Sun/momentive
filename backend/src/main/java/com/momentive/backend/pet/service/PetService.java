package com.momentive.backend.pet.service;

import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.pet.domain.Pet;
import com.momentive.backend.pet.dto.PetListResponse;
import com.momentive.backend.pet.dto.PetRequest;
import com.momentive.backend.pet.dto.PetResponse;
import com.momentive.backend.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetListResponse getPets(Long userId) {
        return PetListResponse.from(petRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PetResponse::from)
                .toList());
    }

    @Transactional
    public PetResponse createPet(Long userId, PetRequest request) {
        User user = getUser(userId);
        Pet pet = Pet.create(user, request.name(), request.breed(), request.birthDate(), request.gender(), request.weightKg());
        petRepository.save(pet);
        return PetResponse.from(pet);
    }

    @Transactional
    public PetResponse updatePet(Long userId, Long petId, PetRequest request) {
        Pet pet = getOwnedPet(userId, petId);
        pet.update(request.name(), request.breed(), request.birthDate(), request.gender(), request.weightKg());
        return PetResponse.from(pet);
    }

    @Transactional
    public void deletePet(Long userId, Long petId) {
        Pet pet = getOwnedPet(userId, petId);
        petRepository.delete(pet);
    }

    private Pet getOwnedPet(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new CustomException(ErrorCode.PET_NOT_FOUND));
        if (!pet.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return pet;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
