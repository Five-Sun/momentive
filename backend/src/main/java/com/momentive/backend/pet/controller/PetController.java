package com.momentive.backend.pet.controller;

import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.pet.dto.PetListResponse;
import com.momentive.backend.pet.dto.PetRequest;
import com.momentive.backend.pet.dto.PetResponse;
import com.momentive.backend.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class PetController {

    private final PetService petService;

    @Operation(summary = "반려견 목록 조회")
    @GetMapping
    public PetListResponse getPets(@Parameter(hidden = true) @CurrentUser Long userId) {
        return petService.getPets(userId);
    }

    @Operation(summary = "반려견 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse createPet(
            @Parameter(hidden = true) @CurrentUser Long userId, @Valid @RequestBody PetRequest request) {
        return petService.createPet(userId, request);
    }

    @Operation(summary = "반려견 수정")
    @PatchMapping("/{petId}")
    public PetResponse updatePet(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long petId,
            @Valid @RequestBody PetRequest request) {
        return petService.updatePet(userId, petId, request);
    }

    @Operation(summary = "반려견 삭제")
    @DeleteMapping("/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePet(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long petId) {
        petService.deletePet(userId, petId);
    }
}
