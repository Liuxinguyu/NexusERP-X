package com.nexus.gateway.filter;

import com.nexus.common.context.NexusRequestHeaders;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationGlobalFilterTest {

    private static NexusSecurityProperties testSecurityProps() {
        NexusSecurityProperties props = new NexusSecurityProperties();
        props.getJwt().setSecret("01234567890123456789012345678901");
        return props;
    }

    private static String validAccessToken(JwtTokenProvider tokenProvider) {
        NexusPrincipal principal = new NexusPrincipal(
                1L, "tester", 2L, null,
                10L, 20L, 1,
                List.of(101L, 102L),
                List.of(201L),
                List.of()
        );
        return tokenProvider.createAccessToken(principal);
    }

    private static String jtiFromRaw(String raw, JwtTokenProvider tokenProvider) {
        return tokenProvider.parseTokenClaims(raw).getId();
    }

    @Test
    void noTokenOnProtectedPathReturns401() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/erp/product/list").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void invalidTokenReturns401() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/system/user/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void validTokenButMissingRedisSessionReturns401() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        String raw = validAccessToken(tokenProvider);
        String jti = jtiFromRaw(raw, tokenProvider);

        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);
        when(redisOps.hasKey("login:token:" + jti)).thenReturn(Mono.just(false));

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/system/user/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + raw)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void publicPathAllowsWithoutToken() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertNull(exchange.getResponse().getStatusCode(), "放行时不应写入错误响应状态");
    }

    @Test
    void actuatorHealthIsPublicWithoutToken() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void actuatorEnvIsNotPublicWildcard() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/env").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPathWithValidTokenAndRedisAllowsAndStripsForgedHeaders() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        String raw = validAccessToken(tokenProvider);
        String jti = jtiFromRaw(raw, tokenProvider);

        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);
        when(redisOps.hasKey("login:token:" + jti)).thenReturn(Mono.just(true));

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/system/user/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + raw)
                .header("X-User-Id", "99999")
                .header("X-Tenant-Id", "88888")
                .header("X-Username", "hacker")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> downstream = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            downstream.set(ex.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertNull(exchange.getResponse().getStatusCode());

        org.springframework.http.server.reactive.ServerHttpRequest req = downstream.get();
        assertEquals("1", req.getHeaders().getFirst("X-User-Id"));
        assertEquals("2", req.getHeaders().getFirst(NexusRequestHeaders.TENANT_ID));
        assertEquals("tester", req.getHeaders().getFirst("X-Username"));
        assertEquals("101,102", req.getHeaders().getFirst(NexusRequestHeaders.ACCESSIBLE_SHOP_IDS));
        assertEquals("201", req.getHeaders().getFirst(NexusRequestHeaders.ACCESSIBLE_ORG_IDS));
    }

    @Test
    void redisTimeoutReturns503() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        String raw = validAccessToken(tokenProvider);

        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);
        when(redisOps.hasKey(anyString())).thenReturn(Mono.never());

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/system/user/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + raw)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void redisExceptionReturns503() {
        NexusSecurityProperties props = testSecurityProps();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        String raw = validAccessToken(tokenProvider);

        @SuppressWarnings("unchecked")
        ReactiveRedisOperations<String, String> redisOps = mock(ReactiveRedisOperations.class);
        when(redisOps.hasKey(anyString())).thenReturn(Mono.error(new IllegalStateException("redis connection failed")));

        AuthenticationGlobalFilter filter = new AuthenticationGlobalFilter(tokenProvider, props, redisOps);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/system/user/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + raw)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
}
