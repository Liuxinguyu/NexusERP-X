package com.nexus.gateway.filter;

import com.nexus.common.context.NexusRequestHeaders;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.JwtAuthenticationException;
import com.nexus.common.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisOperations;
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
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 网关统一 JWT 校验；校验通过后向下游写入身份与数据范围相关 Header（与 {@link NexusRequestHeaders} 一致）。
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
    private static final Duration TOKEN_CHECK_TIMEOUT = Duration.ofSeconds(2);

    /** 与 {@code management.endpoints.web.exposure.include} 对齐：仅放行已暴露的探针路径 */
    private static final List<String> PUBLIC_ACTUATOR_PATHS = List.of("/actuator/health", "/actuator/info");

    private final JwtTokenProvider jwtTokenProvider;
    private final NexusSecurityProperties securityProperties;
    private final ReactiveRedisOperations<String, String> reactiveRedisOps;

    public AuthenticationGlobalFilter(JwtTokenProvider jwtTokenProvider,
                                    NexusSecurityProperties securityProperties,
                                    ReactiveRedisOperations<String, String> reactiveRedisOps) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityProperties = securityProperties;
        this.reactiveRedisOps = reactiveRedisOps;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 先清洗可伪造的下游上下文 Header，防止前端伪造 X-* 头绕过越权/越租户校验
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Tenant-Id");
                    headers.remove("X-Username");
                    headers.remove("X-Client-Type");
                    headers.remove("X-Org-Id");
                    headers.remove("X-Shop-Id");
                    headers.remove("X-Data-Scope");
                    headers.remove("X-Accessible-Shop-Ids");
                    headers.remove("X-Accessible-Org-Ids");
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        String path = sanitizedExchange.getRequest().getPath().value();
        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        String header = sanitizedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        String prefix = jwt.getTokenPrefix() != null ? jwt.getTokenPrefix() : "Bearer ";
        if (!StringUtils.hasText(header) || !header.startsWith(prefix)) {
            return unauthorized(sanitizedExchange, "未提供认证令牌");
        }

        String raw = header.substring(prefix.length()).trim();
        if (!StringUtils.hasText(raw)) {
            return unauthorized(sanitizedExchange, "未提供认证令牌");
        }

        return Mono.defer(() -> Mono.fromCallable(() -> jwtTokenProvider.parseTokenClaims(raw))
                        .subscribeOn(Schedulers.parallel()))
                .flatMap(claims -> {
                    String jti = claims.getId();
                    Long userId = claims.get(JwtTokenProvider.CLAIM_USER_ID, Long.class);
                    Long tenantId = claims.get(JwtTokenProvider.CLAIM_TENANT_ID, Long.class);
                    String username = claims.getSubject();
                    String clientType = claims.get(JwtTokenProvider.CLAIM_CLIENT_TYPE, String.class);
                    if (!StringUtils.hasText(jti) || userId == null || tenantId == null || !StringUtils.hasText(username)) {
                        return unauthorized(sanitizedExchange, "令牌已过期或无效");
                    }
                    if (!StringUtils.hasText(clientType)) {
                        clientType = "web";
                    }

                    Long orgId = claims.get(JwtTokenProvider.CLAIM_ORG_ID, Long.class);
                    Integer dataScope = claims.get(JwtTokenProvider.CLAIM_DATA_SCOPE, Integer.class);
                    Long shopId = claims.get(JwtTokenProvider.CLAIM_SHOP_ID, Long.class);

                    final String ct = clientType;
                    final Long oid = orgId;
                    final Integer ds = dataScope;
                    final Long sid = shopId;

                    return reactiveRedisOps.hasKey(TOKEN_REDIS_PREFIX + jti)
                            .timeout(TOKEN_CHECK_TIMEOUT)
                            .flatMap(exists -> {
                                if (!Boolean.TRUE.equals(exists)) {
                                    log.warn("Token 已失效（已注销或被强退），网关拒绝请求 [user={}]", username);
                                    return unauthorized(sanitizedExchange, "令牌已失效，请重新登录");
                                }
                                List<Long> accessibleShopIds = readClaimLongList(claims, JwtTokenProvider.CLAIM_ACCESSIBLE_SHOP_IDS);
                                List<Long> accessibleOrgIds = readClaimLongList(claims, JwtTokenProvider.CLAIM_ACCESSIBLE_ORG_IDS);

                                ServerHttpRequest mutated = sanitizedExchange.getRequest().mutate()
                                        .headers(h -> {
                                            // token 已校验成功，此处写入真实上下文 Header
                                            h.set(NexusRequestHeaders.TENANT_ID, String.valueOf(tenantId));
                                            h.set("X-Username", username);
                                            h.set("X-Client-Type", ct);
                                            h.set("X-User-Id", String.valueOf(userId));
                                            if (oid != null) {
                                                h.set(NexusRequestHeaders.ORG_ID, String.valueOf(oid));
                                            }
                                            if (ds != null) {
                                                h.set(NexusRequestHeaders.DATA_SCOPE, String.valueOf(ds));
                                            }
                                            if (sid != null) {
                                                h.set(NexusRequestHeaders.SHOP_ID, String.valueOf(sid));
                                            }
                                            if (!accessibleShopIds.isEmpty()) {
                                                h.set(NexusRequestHeaders.ACCESSIBLE_SHOP_IDS, joinIds(accessibleShopIds));
                                            }
                                            if (!accessibleOrgIds.isEmpty()) {
                                                h.set(NexusRequestHeaders.ACCESSIBLE_ORG_IDS, joinIds(accessibleOrgIds));
                                            }
                                        })
                                        .build();
                                return chain.filter(sanitizedExchange.mutate().request(mutated).build());
                            })
                            .onErrorResume(TimeoutException.class, ex -> {
                                log.error("网关校验 Token 时 Redis 响应超时 [user={}]", username, ex);
                                return serviceUnavailable(sanitizedExchange, "认证会话校验超时，请稍后重试");
                            })
                            .onErrorResume(Exception.class, ex -> {
                                log.error("网关校验 Token 时 Redis 发生异常 [user={}]", username, ex);
                                return serviceUnavailable(sanitizedExchange, "认证会话服务暂不可用，请稍后重试");
                            });
                })
                .onErrorResume(JwtAuthenticationException.class,
                        ex -> unauthorized(sanitizedExchange, "令牌已过期或无效"));
    }

    private static boolean isPublicPath(String path) {
        if (path == null) {
            return false; // fail-closed：无法确定路径时拒绝访问
        }
        String normalized = normalizePath(path);
        if (normalized.equals("/api/v1/auth/login") || normalized.equals("/api/v1/auth/confirm-shop")) {
            return true;
        }
        if (normalized.startsWith("/api/v1/system/captcha/")) {
            return true;
        }
        if (PUBLIC_ACTUATOR_PATHS.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("/doc.html")) {
            return true;
        }
        if (normalized.startsWith("/swagger-ui") || normalized.startsWith("/swagger-resources")
                || normalized.startsWith("/v3/api-docs") || normalized.startsWith("/v2/api-docs")
                || normalized.startsWith("/webjars/")) {
            return true;
        }
        return false;
    }

    private static String normalizePath(String path) {
        if (path == null || path.length() <= 1) {
            return path;
        }
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        return jsonError(exchange, HttpStatus.UNAUTHORIZED, 401, msg);
    }

    private static Mono<Void> serviceUnavailable(ServerWebExchange exchange, String msg) {
        return jsonError(exchange, HttpStatus.SERVICE_UNAVAILABLE, 503, msg);
    }

    private static Mono<Void> jsonError(ServerWebExchange exchange, HttpStatus httpStatus, int code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + code + ",\"msg\":\"" + escapeJson(msg) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private static List<Long> readClaimLongList(Claims claims, String claimKey) {
        List<Number> raw = claims.get(claimKey, List.class);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>(raw.size());
        for (Number n : raw) {
            if (n != null) {
                out.add(n.longValue());
            }
        }
        return out;
    }

    private static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
