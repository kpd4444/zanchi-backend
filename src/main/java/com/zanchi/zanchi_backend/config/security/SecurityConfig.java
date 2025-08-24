package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.config.jwt.JwtAuthenticationFilter;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.dev.ignore-redis:false}")
    private boolean ignoreRedisErrorsInDev;


    @Value("${app.cors.allowed-origins:https://zanchi-frontend.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 정적 리소스 공개
                        .requestMatchers(
                                "/", "/index.html", "/signup.html", "/login.html", "/members.html",
                                "/reservation-test.html",
                                "/tags-test.html","/show.html",
                                "/api/members/*/summary"
                        ).permitAll()

                        // 로그인/가입 등 공개 API
                        .requestMatchers(
                                "/api/login", "/api/auth/**", "/api/signup",
                                "/api/members", "/api/members/**",
                                "/api/shows/**"
                        ).permitAll()

                        // ====== [여기 추가] S3 Presign (임시 공개) ======
                        .requestMatchers(HttpMethod.POST, "/api/s3/presign-put", "/s3/presign-put").permitAll()

                        // ====== [클립/태그 공개 엔드포인트] ======
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clips/feed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clips/*/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/view").permitAll()

                        // ====== [클립/태그 인증 필요한 엔드포인트] ======
                        .requestMatchers(HttpMethod.POST, "/api/clips").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments/*/replies").authenticated()

                        // 예약 API
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").authenticated()

                        // 이름 변경 API
                        .requestMatchers(HttpMethod.PATCH,"/api/name").authenticated()

                        .requestMatchers(HttpMethod.POST,"/api/preferences/survey").authenticated()

                        // 그 외 모두 인증
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, memberDetailsService, redisTemplate, ignoreRedisErrorsInDev),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 쉼표로 분리된 오리진 목록 구성
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration config = new CorsConfiguration();

        // 와일드카드(*)가 포함되어 있으면 패턴 API 사용, 아니면 정확 매칭 API 사용
        boolean usePatterns = origins.stream().anyMatch(o -> o.contains("*"));
        if (usePatterns) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOrigins(origins);
        }

        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS","HEAD"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","Accept","Origin"));
        // 프론트에서 Authorization 헤더를 읽어야 한다면 노출
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true); // 쿠키/인증정보 포함 요청 허용
        config.setMaxAge(Duration.ofHours(1)); // preflight 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
