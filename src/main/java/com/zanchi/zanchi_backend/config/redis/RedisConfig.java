package com.zanchi.zanchi_backend.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(); // 기본 값이 사용자의 localhost 값 , Redis 의 포트 값 6379
                                               // 배포할 때는 꼭 host와 port를 지정해줘야 한다. yal 파일에서 명시할 수 있음!! 꼭 참고
                                               // 운영 Redis에는 종종 requirepass 설정이 걸려 있다. 따라서 포트 설정 이외에도 비밀번호 설정을 추가해야 할 수 있다.
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
