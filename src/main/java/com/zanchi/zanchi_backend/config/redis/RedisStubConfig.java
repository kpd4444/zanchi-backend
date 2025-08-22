package com.zanchi.zanchi_backend.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * H2 프로필에서만 등록되는 "더미" Redis 빈.
 * 실제 Redis 서버가 없어도 애플리케이션이 부팅되도록 함.
 * 주의: 실제로 템플릿을 사용하면 연결 예외가 날 수 있으니
 *      RDS/Redis 붙이기 전까지는 해당 기능 호출 금지!
 */
@Configuration
@Profile("h2")
public class RedisStubConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // 시작 시 연결 검증하지 않음(사용 시점에만 연결 시도)
        LettuceConnectionFactory f = new LettuceConnectionFactory("127.0.0.1", 6379);
        f.setValidateConnection(false);
        return f;
    }

    // 자동구성과 동일한 이름으로 제공: 'redisTemplate'
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory f) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(f);
        StringRedisSerializer s = new StringRedisSerializer();
        t.setKeySerializer(s);
        t.setValueSerializer(s);
        t.setHashKeySerializer(s);
        t.setHashValueSerializer(s);
        return t;
    }

    // 필요로 하는 컴포넌트가 있을 수 있으므로 함께 제공
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory f) {
        return new StringRedisTemplate(f);
    }
}
