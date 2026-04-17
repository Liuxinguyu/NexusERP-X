package com.nexus.gateway.filter;

import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.JwtAuthenticationException;
import com.nexus.common.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关统一 JWT 校验；校验通过后向下游写入 X-User-Id、X-Tenant-Id、X-Username、X-Client-Type。
 * <p>
 * 除 JWT 签名校验外，还会检查 token 是否仍存在于 Redis 在线会话白名单中
 * （key 前缀 {@code login:token:}），确保注销/强退后的 token 在网关层即被拦截，
 * 而不仅仅依赖下游各服务的 {@code OnlineTokenValidator}。
 * <p>
 * 优先级高于 {@link DownstreamContextForwardingFilter}。
 */
@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationGlobalFilter.class);

    /** 与 nexus-system OnlineUserRedisService.TOKEN_PREFIX 保持一致 */
    private static final String TOKEN_REDIS_PREFIX = "login:token:";

    private final JwtTokenProvider jwtTokenProvider;
    private final NexusSecurityProperties securityProperties;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;

    public AuthenticationGlobalFilter(JwtTokenProvider jwtTokenProvider,
                                    NexusSecurityProperties securityProperties,
                                    ReactiveStringRedisTemplate reactiveRedisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityProperties = securityProperties;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        String prefix = jwt.getTokenPrefix() != null ? jwt.getTokenPrefix() : "Bearer ";
        if (!StringUtils.hasText(header) || !header.startsWith(prefix)) {
            return unauthorized(exchange, "未提供认证令牌");
        }

        String raw = header.substring(prefix.length()).trim();
        if (!StringUtils.hasText(raw)) {
            return unauthorized(exchange, "未提供认证令牌");
        }

        final Claims claims;
        try {
            claims = jwtTokenProvider.parseTokenClaims(raw);
        } catch (JwtAuthenticationException ex) {
            return unauthorized(exchange, "令牌已过期或无效");
        }

        Long userId = claims.get(JwtTokenProvider.CLAIM_USER_ID, Long.class);
        Long tenantId = claims.get(JwtTokenProvider.CLAIM_TENANT_ID, Long.class);
        String username = claims.getSubject();
        String clientType = claims.get(JwtTokenProvider.CLAIM_CLIENT_TYPE, String.class);
        if (userId == null || tenantId == null || !StringUtils.hasText(username)) {
            return unauthorized(exchange, "令牌已过期或无效");
        }
        if (!StringUtils.hasText(clientType)) {
            clientType = "web";
        }

        Long orgId = claims.get(JwtTokenProvider.CLAIM_ORG_ID, Long.class);
        Integer dataScope = claims.get(JwtTokenProvider.CLAIM_DATA_SCOPE, Integer.class);
        Long shopId = claims.get("sid", Long.class);

        // --- 在线会话白名单校验：确保 token 未被注销/强退 ---
        final String ct = clientType;
        final Long oid = orgId;
        final Integer ds = dataScope;
        final Long sid = shopId;

        return reactiveRedisTemplate.hasKey(TOKEN_REDIS_PREFIX + raw)
                .flatMap(exists -> {
                    if (!Boolean.TRUE.equals(exists)) {
                        log.warn("Token 已失效（已注销或被强退），网关拒绝请求 [user={}]", username);
                        return unauthorized(exchange, "令牌已失效，请重新登录");
                    }
                    ServerHttpRequest.Builder b = exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-Tenant-Id", String.valueOf(tenantId))
                            .header("X-Username", username)
                            .header("X-Client-Type", ct);
                    if (oid != null) {
                        b.header("X-Org-Id", String.valueOf(oid));
                    }
                    if (ds != null) {
                        b.header("X-Data-Scope", String.valueOf(ds));
                    }
                    if (sid != null) {
                        b.header("X-Shop-Id", String.valueOf(sid));
                    }
                    ServerHttpRequest mutated = b.build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private static boolean isPublicPath(String path) {
        if (path == null) {
            return true;
        }
        if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/confirm-shop")) {
            return true;
        }
        if (path.startsWith("/api/v1/system/captcha/")) {
            return true;
        }
        if (path.startsWith("/actuator/")) {
            return true;
        }
        if (path.startsWith("/doc.html")) {
            return true;
        }
        if (path.startsWith("/swagger-ui") || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs") || path.startsWith("/v2/api-docs")
                || path.startsWith("/webjars/")) {
            return true;
        }
        return false;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"msg\":\"" + escapeJson(msg) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
