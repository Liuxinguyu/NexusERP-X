package com.nexus.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * 网关 WebFlux 安全：关闭 CSRF、无状态；authorizeExchange 使用 permitAll 仅表示「允许请求进入 Gateway 过滤链」。
 * 对外的未认证默认拒绝由 {@link com.nexus.gateway.filter.AuthenticationGlobalFilter} 完成（非白名单路径须合法 JWT 且通过 Redis 在线态校验），
 * 即业务层 fail-closed 不依赖此处 denyAll（若在此 denyAll，请求将无法到达自定义 GlobalFilter，除非把鉴权整体迁入 Spring Security）。
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        http.authorizeExchange(ex -> {
            ex.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            ex.anyExchange().permitAll();
        });
        return http.build();
    }
}
