package com.nexus.common.aspect;

import com.nexus.common.annotation.Idempotent;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@Order(90)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IdempotentAspect {

    private static final String KEY_PREFIX = "idempotent:";

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotentAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        String uri = request != null ? request.getRequestURI() : "unknown-uri";
        String userOrIp = resolveUserOrIp(request);
        String idempotencyKey = request != null ? request.getHeader("X-Idempotency-Key") : null;
        String key;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            key = KEY_PREFIX + userOrIp + ":ikey:" + idempotencyKey;
        } else {
            key = KEY_PREFIX + userOrIp + ":" + uri;
        }

        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                key,
                String.valueOf(System.currentTimeMillis()),
                idempotent.expireSeconds(),
                TimeUnit.SECONDS
        );
        if (!Boolean.TRUE.equals(ok)) {
            throw new BusinessException(ResultCode.CONFLICT, idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (BusinessException ex) {
            if (idempotent.releaseOnError()) {
                redisTemplate.delete(key);
            }
            throw ex;
        } catch (Throwable ex) {
            if (idempotent.releaseOnError()) {
                redisTemplate.delete(key);
            }
            log.debug("幂等请求执行异常 key={}", key, ex);
            throw ex;
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private static String resolveUserOrIp(HttpServletRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        return clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown-ip";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "unknown-ip";
    }
}
