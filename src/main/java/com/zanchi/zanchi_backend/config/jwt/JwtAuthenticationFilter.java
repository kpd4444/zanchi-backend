package com.zanchi.zanchi_backend.config.jwt;

import com.zanchi.zanchi_backend.config.security.MemberDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter
 * - 정적 경로(/favicon.ico, /css/**, /js/**, /images/**, *.html 등)는 shouldNotFilter()로 완전히 스킵
 * - 토큰이 없거나/유효하지 않으면 그냥 체인 통과(보호 경로에서는 SecurityConfig가 401 처리)
 * - Redis 블랙리스트(로그아웃 토큰 등) 확인은 선택적으로 수행
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final boolean ignoreRedisErrorsInDev;

    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final List<String> STATIC_PATTERNS = List.of(
            "/favicon.ico",
            "/css/**", "/js/**", "/images/**", "/webjars/**", "/static/**",
            "/", "/**/*.html"
    );

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            MemberDetailsService memberDetailsService,
            RedisTemplate<String, String> redisTemplate,
            boolean ignoreRedisErrorsInDev
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberDetailsService = memberDetailsService;
        this.redisTemplate = redisTemplate;
        this.ignoreRedisErrorsInDev = ignoreRedisErrorsInDev;
    }

    // 정적 리소스는 아예 필터를 타지 않게 스킵
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        String uri = request.getRequestURI();
        for (String p : STATIC_PATTERNS) {
            if (matcher.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = jwtTokenProvider.extractAccessTokenFromCookie(request);

            // 토큰이 없으면 인증 없이 그대로 진행 (보호된 URL은 SecurityConfig가 401 처리)
            if (token == null || token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰이 유효하지 않으면 그대로 통과 (보호된 URL에서 401)
            if (!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // (선택) Redis 블랙리스트 확인
            if (redisTemplate != null) {
                try {
                    String black = redisTemplate.opsForValue().get("BL:"+token);
                    if (black != null) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (Exception e) {
                    if (!ignoreRedisErrorsInDev) throw e; // 로컬은 무시 옵션
                }
            }

            // 토큰에서 로그인ID 추출 후 UserDetails 로드
            String loginId = jwtTokenProvider.getLoginIdFromToken(token);
            UserDetails user = memberDetailsService.loadUserByUsername(loginId);

            // SecurityContext 설정
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception ex) {
            // 필터 단계에서 401을 직접 내려버리면 정적/공개 경로도 영향을 받음 → 예외는 무시하고 체인 진행
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
