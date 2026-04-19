package com.nexus.common.security.jwt;

import com.nexus.common.context.DataScopeContext;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.OrgContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.event.JwtTokenAuthenticatedEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 解析 JWT，并将租户、当前组织/店铺、数据权限写入 ThreadLocal（覆盖请求头中的默认值，防止伪造）。
 * 请求处理完成后通过 finally 块清理 ThreadLocal，防止内存泄漏。
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final NexusSecurityProperties securityProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final OnlineTokenValidator onlineTokenValidator;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   NexusSecurityProperties securityProperties,
                                   ApplicationEventPublisher eventPublisher,
                                   @org.springframework.lang.Nullable OnlineTokenValidator onlineTokenValidator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityProperties = securityProperties;
        this.eventPublisher = eventPublisher;
        this.onlineTokenValidator = onlineTokenValidator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        if (!StringUtils.hasText(header) || !header.startsWith(jwt.getTokenPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }

        String raw = header.substring(jwt.getTokenPrefix().length()).trim();
        try {
            NexusPrincipal principal = jwtTokenProvider.parseToken(raw);

            // 若存在在线会话校验器，检查 token 是否仍在白名单中（未被注销/强退）
            if (onlineTokenValidator != null && !onlineTokenValidator.isTokenOnline(raw)) {
                SecurityContextHolder.clearContext();
                log.warn("Token 已失效（已注销或被强退），请求被拒绝");
                filterChain.doFilter(request, response);
                return;
            }

            if (principal.getTenantId() != null) {
                TenantContext.setTenantId(principal.getTenantId());
            }
            GatewayUserContext.setUserId(principal.getUserId());
            GatewayUserContext.setUsername(principal.getUsername());
            OrgContext.setOrgId(principal.getOrgId());
            OrgContext.setShopId(principal.getShopId());
            if (principal.getDataScope() != null) {
                OrgContext.setDataScope(principal.getDataScope());
            }
            OrgContext.setAccessibleOrgIds(principal.getAccessibleOrgIds());
            OrgContext.setAccessibleShopIds(principal.getAccessibleShopIds());

            DataScopeContext.setDataScope(principal.getDataScope());
            DataScopeContext.setDeptId(principal.getOrgId());
            DataScopeContext.setUserId(principal.getUserId());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String ua = request.getHeader("User-Agent");
            String ip = clientIp(request);
            eventPublisher.publishEvent(new JwtTokenAuthenticatedEvent(this, raw, principal, ip, ua));
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT 解析失败，请求被拒绝：{}", ex.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            TenantContext.clear();
            OrgContext.clear();
            DataScopeContext.clear();
            GatewayUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String x = request.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            return x.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}