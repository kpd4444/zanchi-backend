package com.zanchi.zanchi_backend.config.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// S3Config.java
@Configuration
public class S3Config {
    @Bean
    S3Client s3Client(@Value("${AWS_REGION:ap-northeast-2}") String region) {
        return S3Client.builder().region(Region.of(region)).build(); // EC2 Role 사용
    }
}
