package com.zanchi.zanchi_backend.web.ops;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class OpsController {

    private final StringRedisTemplate redis; // String 기반이면 가볍습니다.

    @GetMapping("/ops/redis-ping")
    public String redisPing() {
        redis.opsForValue().set("ping", "pong", Duration.ofSeconds(30));
        String v = redis.opsForValue().get("ping");
        return v == null ? "null" : v; // "pong" 기대
    }
}
