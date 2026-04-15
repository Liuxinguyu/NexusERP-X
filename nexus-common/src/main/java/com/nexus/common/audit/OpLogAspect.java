package com.nexus.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.SecurityUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志切面：ThreadLocal 计时，成功/异常分别落库（参考 RuoYi 思路）。
 */
@Slf4j
@Aspect
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(100)
public class OpLogAspect {

    private static final int MAX_TEXT = 3800;

    private final ObjectProvider<OperLogRecorder> recorderProvider;
    private final ObjectMapper objectMapper;

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    public OpLogAspect(ObjectProvider<OperLogRecorder> recorderProvider, ObjectMapper objectMapper) {
        this.recorderProvider = recorderProvider;
        this.objectMapper = objectMapper;
    }

    @Before("@annotation(opLog)")
    public void doBefore(JoinPoint jp, OpLog opLog) {
        startTime.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(opLog)", returning = "ret")
    public void doAfterReturning(JoinPoint jp, OpLog opLog, Object ret) {
        try {
            record(jp, opLog, ret, null);
        } finally {
            startTime.remove();
        }
    }

    @AfterThrowing(pointcut = "@annotation(opLog)", throwing = "ex")
    public void doAfterThrowing(JoinPoint jp, OpLog opLog, Throwable ex) {
        try {
            record(jp, opLog, null, ex);
        } finally {
            startTime.remove();
        }
    }

    private void record(JoinPoint jp, OpLog opLog, Object jsonResult, Throwable ex) {
        OperLogRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }
        long cost = System.currentTimeMillis() - (startTime.get() != null ? startTime.get() : System.currentTimeMillis());

        String url = "";
        String method = "";
        String ip = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                url = req.getRequestURI();
                method = req.getMethod();
                ip = clientIp(req);
            }
        } catch (Exception ignored) {
        }

        Long tenantId = TenantContext.getTenantId();
        Long userId = null;
        String username = null;
        NexusPrincipal p = SecurityUtils.currentPrincipal();
        if (p != null) {
            userId = p.getUserId();
            username = p.getUsername();
        }
        if (userId == null) {
            userId = GatewayUserContext.getUserId();
        }
        if (username == null) {
            username = GatewayUserContext.getUsername();
        }

        String reqStr = opLog.isSaveRequestData() ? truncate(buildRequestJson(jp, opLog)) : "";
        String respStr = "";
        if (ex == null && opLog.isSaveResponseData() && jsonResult != null) {
            respStr = truncate(writeJson(jsonResult));
        }

        int status = ex == null ? 1 : 0;
        String err = ex == null ? null : truncate(ex.getMessage());

        OperLogRecord record = OperLogRecord.builder()
                .tenantId(tenantId)
                .userId(userId)
                .username(username)
                .module(opLog.module())
                .operType(opLog.type())
                .operUrl(url)
                .operMethod(method)
                .operIp(ip)
                .requestParam(reqStr)
                .responseData(respStr)
                .status(status)
                .errorMsg(err)
                .costTime(cost)
                .build();
        try {
            recorder.record(record);
        } catch (Exception e) {
            log.warn("操作日志落库失败: {}", e.getMessage());
        }
    }

    private static String clientIp(jakarta.servlet.http.HttpServletRequest req) {
        String x = req.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) {
            return x.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String buildRequestJson(JoinPoint jp, OpLog opLog) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method m = sig.getMethod();
        Parameter[] params = m.getParameters();
        Object[] args = jp.getArgs();
        Set<String> exclude = new LinkedHashSet<>();
        for (String s : opLog.excludeParamNames()) {
            exclude.add(s);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < params.length; i++) {
            String name = params[i].getName();
            if (exclude.contains(name)) {
                continue;
            }
            Object v = args[i];
            if (v instanceof ServletRequest || v instanceof ServletResponse) {
                continue;
            }
            map.put(name, v);
        }
        return writeJson(map);
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= MAX_TEXT) {
            return s;
        }
        return s.substring(0, MAX_TEXT) + "...";
    }
}
