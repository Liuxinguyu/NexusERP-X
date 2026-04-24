package com.nexus.gateway.filter;

import com.nexus.common.context.NexusRequestHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 下游上下文转发过滤器。
 *
 * <p>【安全说明】
 * 仅转发 {@link AuthenticationGlobalFilter} 在校验 JWT 与 Redis 在线态后写入的请求头
 * （身份、租户、数据范围、可访问店铺/组织 ID 列表等），与 {@link NexusRequestHeaders} 命名一致。
 * <p>
 * 绝不再将用户传入的请求头（如 tenant-id、user-id 等小写变体）转发给下游服务，
 * 以防止 Header 伪造攻击（攻击者通过传入伪造的租户/用户ID 绕过数据隔离）。
 *
 * @see AuthenticationGlobalFilter
 */
@Component
public class DownstreamContextForwardingFilter implements GlobalFilter, Ordered {

    /**
     * 仅转发网关生成的安全 Header（前缀 X-）。
     * 这些头由 {@link AuthenticationGlobalFilter} 在 JWT 验证后写入，下游服务可以安全信任。
     */
    private static final List<String> FORWARD_HEADER_NAMES = List.of(
            "X-User-Id",
            NexusRequestHeaders.TENANT_ID,
            "X-Username",
            "X-Client-Type",
            NexusRequestHeaders.ORG_ID,
            NexusRequestHeaders.SHOP_ID,
            NexusRequestHeaders.DATA_SCOPE,
            NexusRequestHeaders.ACCESSIBLE_SHOP_IDS,
            NexusRequestHeaders.ACCESSIBLE_ORG_IDS
    );

    /** 是否启用下游 Header 转发（测试场景可关闭） */
    @Value("${nexus.gateway.forward-context:true}")
    private boolean forwardContextEnabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!forwardContextEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // 仅转发网关写入的 X- 前缀 Header，忽略用户传入的所有原始 Header
        ServerHttpRequest newRequest = request.mutate()
                .headers(h -> {
                    for (String name : FORWARD_HEADER_NAMES) {
                        List<String> values = request.getHeaders().getValuesAsList(name).stream()
                                .filter(StringUtils::hasText)
                                .collect(Collectors.toList());
                        if (!values.isEmpty()) {
                            // 逗号分隔的 ID 列表在 HttpHeaders 中可能被拆成多元素；下游需单一字符串时合并
                            if (NexusRequestHeaders.ACCESSIBLE_SHOP_IDS.equals(name)
                                    || NexusRequestHeaders.ACCESSIBLE_ORG_IDS.equals(name)) {
                                h.set(name, String.join(",", values));
                            } else {
                                h.put(name, values);
                            }
                        }
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        // 在 AuthenticationGlobalFilter 之后执行
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}