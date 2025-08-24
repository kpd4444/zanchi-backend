package com.zanchi.zanchi_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;

@Configuration
public class PersonalizeConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public PersonalizeRuntimeClient personalizeRuntimeClient() {
        return PersonalizeRuntimeClient.builder()
                .region(Region.of(region))
                .build(); // 자격증명은 기본 체인 사용 (EC2 역할/환경변수 등)
    }
}
