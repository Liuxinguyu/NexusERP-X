package com.nexus.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.annotation.OpLog;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.SecurityUtils;
import com.nexus.common.utils.HttpRequestUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志切面：使用 @Around 统一计时与成功/异常落库。
 */
@Slf4j
@Aspect
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Order(100)
public class OpLogAspect {

    private static final int MAX_TEXT = 3800;
    private static final String MASK = "******";
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "password", "oldpassword", "newpassword", "secretkey",
            "token", "secret", "credential", "apikey", "accesskey",
            "authorization", "cardnumber", "cvv", "ssn", "idcard"
    );

    private final ObjectProvider<OperLogRecorder> recorderProvider;
    private final ObjectMapper objectMapper;

    public OpLogAspect(ObjectProvider<OperLogRecorder> recorderProvider, ObjectMapper objectMapper) {
        this.recorderProvider = recorderProvider;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint jp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object ret = null;
        Throwable thrown = null;
        try {
            ret = jp.proceed();
            return ret;
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                record(jp, opLog, ret, thrown, cost);
            } catch (Exception e) {
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private void record(JoinPoint jp, OpLog opLog, Object jsonResult, Throwable ex, long cost) {
        OperLogRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }

        String url = "";
        String method = "";
        String ip = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                url = req.getRequestURI();
                method = req.getMethod();
                ip = HttpRequestUtils.clientIp(req);
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
        return writeMaskedJson(map);
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private String writeMaskedJson(Object origin) {
        try {
            Object masked = maskSensitive(origin);
            return objectMapper.writeValueAsString(masked);
        } catch (Exception e) {
            log.error("操作日志参数脱敏失败: {}", e.getMessage(), e);
            return "[WARN] operParam脱敏失败，已跳过原始参数记录";
        }
    }

    @SuppressWarnings("unchecked")
    private Object maskSensitive(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object rawValue = entry.getValue();
                if (isSensitiveKey(key)) {
                    out.put(key, MASK);
                } else {
                    out.put(key, maskSensitive(rawValue));
                }
            }
            return out;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::maskSensitive).toList();
        }
        if (value instanceof Set<?> set) {
            return set.stream().map(this::maskSensitive).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (isPrimitiveLike(value)) {
            return value;
        }
        // 对任意对象先转成 Map，再递归脱敏（不修改原对象）
        Map<String, Object> asMap = objectMapper.convertValue(value, LinkedHashMap.class);
        return maskSensitive(asMap);
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrimitiveLike(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum();
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
