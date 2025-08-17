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

    // 개발 중 Redis 장애 무시 여부(선택)
    @Value("${auth.dev.ignore-redis:false}")
    private boolean ignoreRedisErrorsInDev;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 기본 보안 정책
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 정적/페이지 허용 (테스트용 페이지 포함)
                        .requestMatchers(
                                "/", "/index.html",
                                "/signup.html", "/login.html",
                                "/members.html", "/reservation-test.html",
                                "/tags-test.html", "/notifications.html", "/clip-feed.html",
                                "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/webjars/**"
                        ).permitAll()

                        // 공개 API (회원가입/로그인 등)
                        .requestMatchers(
                                "/api/login", "/api/auth/**", "/api/signup",
                                "/api/shows/**"
                        ).permitAll()

                        // 공개 조회 허용(피드/태그/댓글 조회/뷰 카운트 증가)
                        .requestMatchers(HttpMethod.GET,  "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/clips/feed").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/clips/*/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/view").permitAll()

                        // 반드시 인증 필요한 API
                        // - 내 정보/프로필 관련
                        .requestMatchers("/api/me/**").authenticated()
                        // - 알림 전부
                        .requestMatchers("/api/notifications/**").authenticated()
                        // - 팔로우/언팔/관계
                        .requestMatchers("/api/members/*/follow**").authenticated()
                        .requestMatchers("/api/members/*/relation").authenticated()
                        // - 클립 생성/좋아요/댓글/대댓글
                        .requestMatchers(HttpMethod.POST, "/api/clips").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments/*/replies").authenticated()
                        // - 예약/내 예약
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").authenticated()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // 예외 처리
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403))
                )

                // JWT 필터
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider, memberDetailsService, redisTemplate, ignoreRedisErrorsInDev
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
