package com.momentive.backend.image.controller;

import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.image.dto.ImageUploadSignatureResponse;
import com.momentive.backend.image.service.CloudinarySignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 이미지 업로드 지원 API. {@code /admin/**}이므로 ADMIN 권한이 없으면 403으로 끊긴다.
 */
@RestController
@RequestMapping("/admin/images")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class AdminImageController {

    private final CloudinarySignatureService cloudinarySignatureService;

    @Operation(summary = "[관리자] Cloudinary 업로드 서명 발급")
    @PostMapping("/signature")
    public ImageUploadSignatureResponse issueUploadSignature() {
        return cloudinarySignatureService.issueUploadSignature();
    }
}
