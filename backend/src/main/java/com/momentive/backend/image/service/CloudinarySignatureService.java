package com.momentive.backend.image.service;

import com.momentive.backend.image.config.CloudinaryProperties;
import com.momentive.backend.image.dto.ImageUploadSignatureResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cloudinary signed upload 서명을 발급한다. 파일 바이트는 백엔드를 거치지 않고
 * 브라우저가 이 서명으로 Cloudinary에 직접 올린다.
 *
 * <p>서명은 Cloudinary 규약대로 <b>업로드 파라미터를 키 이름 오름차순으로 이어붙인 문자열</b> 뒤에
 * API secret을 덧붙여 SHA-1로 해싱한 값이다. 현재 서명 대상 파라미터는 {@code folder}, {@code timestamp}
 * 두 개이며 이 순서가 곧 알파벳 순서다.
 */
@Service
@RequiredArgsConstructor
public class CloudinarySignatureService {

    private final CloudinaryProperties properties;

    public ImageUploadSignatureResponse issueUploadSignature() {
        long timestamp = Instant.now().getEpochSecond();
        String folder = properties.uploadFolder();
        String signature = sign("folder=" + folder + "&timestamp=" + timestamp);

        return new ImageUploadSignatureResponse(
                signature,
                timestamp,
                properties.apiKey(),
                properties.cloudName(),
                folder);
    }

    /**
     * {@code paramsToSign + apiSecret}의 SHA-1 해시를 16진 소문자로 돌려준다.
     * secret은 여기서만 쓰이고 응답에는 실리지 않는다.
     */
    private String sign(String paramsToSign) {
        String payload = paramsToSign + nullToEmpty(properties.apiSecret());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-1은 모든 JVM이 제공하므로 실제로는 도달하지 않는다.
            throw new IllegalStateException("SHA-1 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
