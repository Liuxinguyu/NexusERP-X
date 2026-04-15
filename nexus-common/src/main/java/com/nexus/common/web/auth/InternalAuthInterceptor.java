package com.nexus.common.web.auth;

import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 防止绕过网关直连业务服务：要求请求携带网关解析 JWT 后写入的 X-User-Id、X-Tenant-Id。
 * <p>
 * 通过 finally 块确保 ThreadLocal 清理，防止内存泄漏。
 */
@Slf4j
public class InternalAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USERNAME = "X-Username";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();
        if (isExcluded(path)) {
            return true;
        }
        if (!path.startsWith("/api/v1/")) {
            return true;
        }

        String userId = request.getHeader(HEADER_USER_ID);
        String tenantId = request.getHeader(HEADER_TENANT_ID);
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(tenantId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"禁止绕过网关访问\"}");
            return false;
        }
        long tid;
        try {
            tid = Long.parseLong(tenantId.trim());
        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"租户上下文无效\"}");
            return false;
        }
        long uid;
        try {
            uid = Long.parseLong(userId.trim());
        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"用户上下文无效\"}");
            return false;
        }
        TenantContext.setTenantId(tid);
        GatewayUserContext.setUserId(uid);
        String xu = request.getHeader(HEADER_USERNAME);
        if (StringUtils.hasText(xu)) {
            GatewayUserContext.setUsername(xu.trim());
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        TenantContext.clear();
        GatewayUserContext.clear();
    }

    private static boolean isExcluded(String path) {
        if (path == null) {
            return true;
        }
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }
        if (path.startsWith("/api/v1/system/captcha/")) {
            return true;
        }
        if (path.startsWith("/actuator/") || path.startsWith("/error")) {
            return true;
        }
        if (path.startsWith("/doc.html") || path.startsWith("/swagger-ui") || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs") || path.startsWith("/v2/api-docs") || path.startsWith("/webjars/")) {
            return true;
        }
        return false;
    }
}