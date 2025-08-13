package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.config.jwt.JwtAuthenticationFilter;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    // application.properties에서 주입
    @Value("${auth.dev.ignore-redis:false}")
    private boolean ignoreRedisErrorsInDev;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 리소스
                        .requestMatchers(
                                "/", "/index.html", "/signup.html", "/login.html", "/members.html",
                                "/api/login", "/api/auth/**", "/api/signup", "/api/members", "/api/members/**",
                                "/api/shows/**",
                                "/reservation-test.html"                  // ← 테스트 페이지 허용
                        ).permitAll()

                        // 인증 필요 API
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").authenticated()  // ← 수정

                        // 그 외 모두 인증
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401)) // 인증 X
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403))      // 인증 O, 권한 X
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, memberDetailsService, redisTemplate, ignoreRedisErrorsInDev),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
