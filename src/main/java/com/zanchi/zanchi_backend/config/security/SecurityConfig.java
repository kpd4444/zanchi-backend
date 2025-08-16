package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.config.jwt.JwtAuthenticationFilter;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.dev.ignore-redis:false}")
    private boolean ignoreRedisErrorsInDev;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 정적 리소스(필요한 페이지만 허용 중)
                        .requestMatchers(
                                "/", "/index.html", "/signup.html", "/login.html", "/members.html",
                                "/reservation-test.html",
                                "/tags-test.html"            // ← 태그 테스트 페이지 공개
                        ).permitAll()

                        // 로그인/가입 등 공개 API
                        .requestMatchers(
                                "/api/login", "/api/auth/**", "/api/signup",
                                "/api/members", "/api/members/**",
                                "/api/shows/**"
                        ).permitAll()

                        // ====== [클립/태그 공개 엔드포인트] ======
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()                 // 태그 목록/검색
                        .requestMatchers(HttpMethod.GET, "/api/clips/feed").permitAll()              // 피드
                        .requestMatchers(HttpMethod.GET, "/api/clips/*/comments/**").permitAll()     // 댓글/대댓글 조회
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/view").permitAll()           // 조회수 증가

                        // ====== [클립/태그 인증 필요한 엔드포인트] ======
                        .requestMatchers(HttpMethod.POST, "/api/clips").authenticated()              // 업로드
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/like").authenticated()       // 좋아요 토글
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments").authenticated()   // 댓글 작성
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments/*/replies").authenticated() // 대댓글

                        // 예약 API (기존 정책 유지)
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").authenticated()

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
}
