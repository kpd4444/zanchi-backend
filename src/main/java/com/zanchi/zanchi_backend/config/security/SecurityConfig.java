package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.config.jwt.JwtAuthenticationFilter;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .httpBasic(AbstractHttpConfigurer::disable)
//                .formLogin(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(
//                        new JwtAuthenticationFilter(jwtTokenProvider, memberDetailsService),
//                        UsernamePasswordAuthenticationFilter.class
//                );
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 리소스
                        .requestMatchers("/**").permitAll()

                        // 클립 API는 '인증만' 요구
                        .requestMatchers("/api/clips/**").authenticated()

                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req,res,ex) -> res.sendError(401)) // 인증 X → 401
                        .accessDeniedHandler((req,res,ex) -> res.sendError(403))      // 인증 O 권한 X → 403
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, memberDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
