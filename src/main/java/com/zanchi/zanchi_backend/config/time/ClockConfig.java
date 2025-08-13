package com.zanchi.zanchi_backend.config.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {
    @Bean
    public Clock kstClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
