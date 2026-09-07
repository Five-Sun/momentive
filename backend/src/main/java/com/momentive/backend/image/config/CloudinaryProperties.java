package com.momentive.backend.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudinary signed upload 설정. 값은 전부 환경변수로 주입하며 코드·설정 파일에 실제 값을 두지 않는다.
 *
 * <p>자격증명이 아직 없는 환경에서도 애플리케이션 기동과 테스트가 깨지지 않도록
 * {@code application.yml}에서 빈 문자열 기본값을 준다. 이 경우 서명은 계산되지만
 * Cloudinary가 거부하므로, 실제 업로드는 자격증명을 넣은 뒤에 동작한다.
 */
@ConfigurationProperties(prefix = "momentive.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String uploadFolder
) {
}
