package com.momentive.backend.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cloudinary signed upload에 필요한 값 묶음. 브라우저가 이 값으로 Cloudinary에 직접 업로드한다.
 *
 * <p>API secret은 서명 계산에만 쓰이고 이 응답에는 포함되지 않는다.
 */
public record ImageUploadSignatureResponse(
        @Schema(description = "업로드 파라미터로 계산된 SHA-1 서명(16진 소문자)") String signature,
        @Schema(description = "서명에 사용된 UNIX epoch 초. 업로드 요청에 그대로 실어야 한다") long timestamp,
        @Schema(description = "Cloudinary API key") String apiKey,
        @Schema(description = "Cloudinary cloud name") String cloudName,
        @Schema(description = "업로드 대상 폴더") String folder
) {
}
