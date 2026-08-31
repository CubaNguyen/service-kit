package com.servicekit.security.filter;

import com.servicekit.security.context.AuthContext;
import com.servicekit.security.context.AuthContextHolder;
import com.servicekit.security.revocation.TokenRevocationStore;
import com.servicekit.security.token.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter chặn mọi request đi vào hệ thống để trích xuất, xác thực Bearer Token (JWT),
 * và kiểm tra trạng thái thu hồi (revoked) trước khi thiết lập context bảo mật.
 */
@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenRevocationStore revocationStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                AuthContext authContext = tokenProvider.parseToken(token);

                // Kiểm tra 2 lớp thu hồi:
                // 1. Kiểm tra jti token đơn lẻ (thiết bị) bị blacklist hay chưa.
                // 2. Kiểm tra mốc cutoff thu hồi toàn bộ token của User đó.
                Instant cutoff = revocationStore.getRevokeAllCutoff(authContext.userId());
                boolean isRevoked = revocationStore.isTokenRevoked(authContext.jti())
                        || (cutoff != null && authContext.issuedAt() != null && authContext.issuedAt().isBefore(cutoff));

                if (isRevoked) {
                    log.warn("[TokenAuthenticationFilter] Blocked revoked token: jti={}, userId={}",
                            authContext.jti(), authContext.userId());
                } else {
                    // Thiết lập local ThreadLocal AuthContext
                    AuthContextHolder.setContext(authContext);

                    // Map roles & permissions sang Spring Security GrantedAuthorities
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (authContext.roles() != null) {
                        authContext.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                    if (authContext.permissions() != null) {
                        authContext.permissions().forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            authContext, null, authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Thiết lập bảo mật cho Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.debug("[TokenAuthenticationFilter] Token authentication failed: {}", e.getMessage());
            // Tránh ném Exception ở đây để các URL permitAll có thể truy cập ẩn danh bình thường
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Dọn sạch local thread context để tránh rò rỉ bộ nhớ giữa các luồng tái sử dụng của Tomcat
            AuthContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
