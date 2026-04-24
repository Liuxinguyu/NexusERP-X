package com.nexus.gateway.filter;

import com.nexus.common.context.NexusRequestHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DownstreamContextForwardingFilterTest {

    @Test
    void forwardsAllowlistedHeadersFromGatewayRequest() {
        DownstreamContextForwardingFilter filter = new DownstreamContextForwardingFilter();
        ReflectionTestUtils.setField(filter, "forwardContextEnabled", true);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/erp/x")
                .header("X-User-Id", "1")
                .header(NexusRequestHeaders.TENANT_ID, "2")
                .header("X-Username", "u")
                .header("X-Client-Type", "web")
                .header(NexusRequestHeaders.ORG_ID, "20")
                .header(NexusRequestHeaders.SHOP_ID, "10")
                .header(NexusRequestHeaders.DATA_SCOPE, "1")
                .header(NexusRequestHeaders.ACCESSIBLE_SHOP_IDS, "101,102")
                .header(NexusRequestHeaders.ACCESSIBLE_ORG_IDS, "201")
                .header("X-Custom-Trace", "trace-1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> next = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            next.set(ex.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));

        org.springframework.http.server.reactive.ServerHttpRequest out = next.get();
        assertNotNull(out);
        HttpHeaders h = out.getHeaders();
        assertEquals("1", h.getFirst("X-User-Id"));
        assertEquals("2", h.getFirst(NexusRequestHeaders.TENANT_ID));
        assertEquals("101,102", h.getFirst(NexusRequestHeaders.ACCESSIBLE_SHOP_IDS));
        assertEquals("201", h.getFirst(NexusRequestHeaders.ACCESSIBLE_ORG_IDS));
        assertEquals("trace-1", h.getFirst("X-Custom-Trace"));
    }

    @Test
    void whenForwardingDisabledPassesThroughUnchanged() {
        DownstreamContextForwardingFilter filter = new DownstreamContextForwardingFilter();
        ReflectionTestUtils.setField(filter, "forwardContextEnabled", false);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/erp/x")
                .header(NexusRequestHeaders.TENANT_ID, "2")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> next = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            next.set(ex.getRequest());
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(request, next.get());
    }
}
