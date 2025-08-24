package com.zanchi.zanchi_backend.config.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class PresignConfig {

    @Bean
    public S3Client s3Client() {
        // 자격증명은 기본 프로바이더 체인(EC2 role 등) 사용
        return S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}
