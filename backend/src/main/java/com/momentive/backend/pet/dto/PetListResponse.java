package com.momentive.backend.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PetListResponse(
        @Schema(description = "반려견 목록") List<PetResponse> pets
) {

    public static PetListResponse from(List<PetResponse> pets) {
        return new PetListResponse(pets);
    }
}
