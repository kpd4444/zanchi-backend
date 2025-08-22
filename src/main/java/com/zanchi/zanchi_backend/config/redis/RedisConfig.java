package com.zanchi.zanchi_backend.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Profile("!h2")   // ⬅️ h2 프로필일 때는 이 설정을 로드하지 않음
public class RedisConfig {

    /*
     * spring.data.redis.* 값을 가져옵니다.
     * - application.yml / properties 에서 설정하면 주입됩니다.
     * - 기본값(default)으로 localhost:6379, password는 없음("") 처리합니다.
     *   => 로컬 개발 환경에서 Docker로 redis를 띄운 경우 기본값만으로도 접속 가능.
     *   => 배포/운영 환경에서는 반드시 실제 호스트/포트/패스워드를 설정하세요.
     *
     * 예) application.yml
     * spring:
     *   data:
     *     redis:
     *       host: 127.0.0.1
     *       port: 6379
     *       password: yourStrongPassword   # 운영에서 requirepass 사용 시
     */
    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * RedisConnectionFactory
     *
     * - Lettuce 클라이언트를 사용하여 Redis 연결 팩토리를 생성합니다.
     * - 'Standalone' 구성(싱글 노드) 기준이며, Sentinel/Cluster가 아니라면 일반적으로 이 구성을 사용합니다.
     * - commandTimeout/shutdownTimeout 등 클라이언트 타임아웃을 명시해
     *   서버 미가동 시 호출이 과도하게 지연되는 문제를 방지합니다.
     * - password가 설정된 경우에만 RedisPassword를 적용합니다.
     *
     * 주의:
     * - 이 Bean 생성 자체는 실제 연결을 시도하지 않습니다(지연 연결).
     *   Redis가 꺼져 있어도 애플리케이션은 기동되지만,
     *   실제 명령을 수행하는 시점에 연결 예외가 발생할 수 있습니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 1) 서버 접속 정보(호스트/포트/패스워드)
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(RedisPassword.of(password));
        }

        // 2) 클라이언트 동작 옵션(타임아웃 등)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))   // Redis 명령 타임아웃(필요시 조정)
                .shutdownTimeout(Duration.ZERO)          // 애플리케이션 종료 시 대기 시간
                .build();

        // 3) 최종 ConnectionFactory
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * RedisTemplate<String, String>
     *
     * - 키/값 모두 '문자열'로 다루는 템플릿입니다(블랙리스트 토큰, 간단한 플래그 등에 적합).
     * - 직렬화기(Serializer)를 String 기반으로 통일하여
     *   Redis CLI로 조회 시 사람이 읽기 쉬운 형태가 됩니다.
     *
     * 확장:
     * - 객체를 JSON으로 저장하고 싶다면 value/hashValue에
     *   GenericJackson2JsonRedisSerializer 를 설정하세요.
     *   (단, 클래스 구조 변경 시 역직렬화 이슈에 유의)
     *
     * 트랜잭션:
     * - 기본적으로 트랜잭션은 비활성화입니다.
     *   분산락/원자적 갱신이 필요하면 별도 설정이나 Lua 스크립트를 고려하세요.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 모든 영역에 동일한 문자열 직렬화기를 적용
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);        // 일반 Key
        template.setValueSerializer(stringSerializer);      // 일반 Value
        template.setHashKeySerializer(stringSerializer);    // Hash Key
        template.setHashValueSerializer(stringSerializer);  // Hash Value

        // afterPropertiesSet()을 호출하여 설정을 마무리
        template.afterPropertiesSet();
        return template;
    }
}
