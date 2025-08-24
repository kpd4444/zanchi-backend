package com.zanchi.zanchi_backend.config.jwt;

import com.zanchi.zanchi_backend.config.security.MemberDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;

    /**
     * 운영에서는 반드시 주입, 개발에서는 null 가능
     */
    @Nullable
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 개발 모드에서 Redis 장애/미기동 시 무시 여부
     */
    private final boolean ignoreRedisErrorsInDev;

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String BLACKLIST_PREFIX = "LOGOUT:";

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   MemberDetailsService memberDetailsService,
                                   @Nullable RedisTemplate<String, String> redisTemplate,
                                   @Value("${auth.dev.ignore-redis:false}") boolean ignoreRedisErrorsInDev) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberDetailsService = memberDetailsService;
        this.redisTemplate = redisTemplate;
        this.ignoreRedisErrorsInDev = ignoreRedisErrorsInDev;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && validateSafely(token)) {

            if (isBlacklisted(token)) {
                log.warn("Blocked token by blacklist.");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                return;
            }

            String loginId = jwtTokenProvider.getLoginIdFromToken(token);
            var userDetails = memberDetailsService.loadUserByUsername(loginId);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더(Bearer) → 쿠키 순서 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer)) {
            bearer = bearer.trim();
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7).trim();
            }
            if (bearer.split("\\.").length == 3) {
                return bearer;
            }
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (ACCESS_TOKEN_COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /** 토큰 검증 시 예외 흡수 */
    private boolean validateSafely(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            log.warn("Token validation error", e);
            return false;
        }
    }

    /**
     * 블랙리스트 체크
     * - dev 환경에서 ignoreRedisErrorsInDev=true이면 조회 자체를 건너뜀
     * - 운영에서도 redis 미구성 시 막지 말고 통과(원하면 block 로직으로 변경)
     * - Redis 오류 시: dev는 통과, prod는 차단
     */
    private boolean isBlacklisted(String token) {
        // 개발에서는 블랙리스트 체크 생략
        if (ignoreRedisErrorsInDev) {
            return false;
        }

        // 운영에서도 redis 미구성 시 막지 말고 통과 (원하면 true 로 바꾸세요)
        if (redisTemplate == null) {
            log.warn("RedisTemplate is null → skip blacklist check (treat as not blacklisted)");
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            // 운영에서 Redis 장애 시 막고 싶으면 true, 개발은 property 로 false
            log.warn("Redis error in blacklist check", e);
            return !ignoreRedisErrorsInDev;
        }
    }
}
