package com.momentive.backend.pet.dto;

import com.momentive.backend.pet.domain.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PetRequest(
        @Schema(description = "이름") @NotBlank String name,
        @Schema(description = "품종") String breed,
        @Schema(description = "생일") LocalDate birthDate,
        @Schema(description = "성별") PetGender gender,
        @Schema(description = "몸무게(kg)") Double weightKg
) {
}
