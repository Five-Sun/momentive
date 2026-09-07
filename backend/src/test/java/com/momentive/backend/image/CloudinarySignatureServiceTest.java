package com.momentive.backend.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.momentive.backend.image.dto.ImageUploadSignatureResponse;
import com.momentive.backend.image.service.CloudinarySignatureService;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "momentive.cloudinary.cloud-name=test-cloud",
        "momentive.cloudinary.api-key=123456789012345",
        "momentive.cloudinary.api-secret=super-secret-value",
        "momentive.cloudinary.upload-folder=momentive/test"
})
class CloudinarySignatureServiceTest {

    private static final String API_SECRET = "super-secret-value";

    @Autowired
    private CloudinarySignatureService cloudinarySignatureService;

    /**
     * 헬퍼 자체가 SHA-1을 제대로 계산하는지 널리 알려진 테스트 벡터로 먼저 고정한다.
     * 아래 서명 검증이 "같은 버그를 양쪽에서 반복"하는 검증이 되지 않도록 하기 위함이다.
     */
    @Test
    void sha1Hex_helper_matches_the_well_known_test_vector() {
        assertThat(sha1Hex("abc")).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
    }

    @Test
    void issueUploadSignature_signs_folder_and_timestamp_with_the_api_secret() {
        ImageUploadSignatureResponse response = cloudinarySignatureService.issueUploadSignature();

        assertThat(response.cloudName()).isEqualTo("test-cloud");
        assertThat(response.apiKey()).isEqualTo("123456789012345");
        assertThat(response.folder()).isEqualTo("momentive/test");
        assertThat(response.timestamp()).isCloseTo(Instant.now().getEpochSecond(), within(60L));

        // Cloudinary 규약: 서명 대상 파라미터를 키 이름 오름차순으로 이어붙인 뒤 API secret을 덧붙여 SHA-1.
        String expected = sha1Hex("folder=momentive/test&timestamp=" + response.timestamp() + API_SECRET);
        assertThat(response.signature()).isEqualTo(expected);
    }

    /**
     * spec 수용 기준: API secret이 응답에 포함되지 않아야 한다.
     */
    @Test
    void issueUploadSignature_never_exposes_the_api_secret() {
        ImageUploadSignatureResponse response = cloudinarySignatureService.issueUploadSignature();

        assertThat(Arrays.stream(ImageUploadSignatureResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("apiSecret", "secret");
        assertThat(response.toString()).doesNotContain(API_SECRET);
        assertThat(response.signature()).doesNotContain(API_SECRET);
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
