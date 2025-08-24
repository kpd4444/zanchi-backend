package com.zanchi.zanchi_backend.reco;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;

@Configuration
public class PersonalizeRuntimeConfig {
    @Bean
    public PersonalizeRuntimeClient personalizeRuntimeClient() {
        return PersonalizeRuntimeClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}
