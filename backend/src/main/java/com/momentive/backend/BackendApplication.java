package com.momentive.backend;

import com.momentive.backend.image.config.CloudinaryProperties;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(CloudinaryProperties.class)
@SpringBootApplication
public class BackendApplication {

    /**
     * 애플리케이션 기준 시간대를 KST로 고정한다.
     *
     * <p>쿠폰 유효기간처럼 {@code LocalDateTime}으로 저장·비교하는 값은 JVM 기본 시간대를 따르는데,
     * Railway 컨테이너는 UTC로 뜨고 고객은 KST를 쓴다. 고정하지 않으면 서버가 판정하는 만료 시각과
     * 화면에 보이는 만료 시각이 9시간 어긋난다.
     */
    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
