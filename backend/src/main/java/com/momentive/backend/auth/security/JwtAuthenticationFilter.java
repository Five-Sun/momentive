package com.momentive.backend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 쿠키의 access token을 검증해 SecurityContext에 인증 정보를 채운다.
 * 토큰이 없거나 유효하지 않아도 여기서 401을 내리지 않고 통과시킨다 —
 * 인증이 필요한 엔드포인트의 실제 거부는 SecurityFilterChain의 인가 설정과
 * AuthenticationEntryPoint가 담당한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieProvider authCookieProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthCookieProvider authCookieProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authCookieProvider = authCookieProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> accessToken = authCookieProvider.getAccessToken(request);
        accessToken.flatMap(jwtTokenProvider::parseAccessToken)
                .ifPresent(payload -> {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            payload.userId(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + payload.role().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        filterChain.doFilter(request, response);
    }
}
