package com.momentive.backend.pet.dto;

import com.momentive.backend.pet.domain.Pet;
import com.momentive.backend.pet.domain.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PetResponse(
        @Schema(description = "반려견 ID") Long id,
        @Schema(description = "이름") String name,
        @Schema(description = "품종") String breed,
        @Schema(description = "생일") LocalDate birthDate,
        @Schema(description = "성별") PetGender gender,
        @Schema(description = "몸무게(kg)") Double weightKg
) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.getGender(),
                pet.getWeightKg()
        );
    }
}
