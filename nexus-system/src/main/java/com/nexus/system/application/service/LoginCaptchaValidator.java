package com.nexus.system.application.service;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 登录验证码校验。
 * <p>
 * 验证码存储在 Redis，key 格式为 {@code captcha:{captchaKey}}，value 为验证码文本。
 * 仅在 {@code sys.account.captchaEnabled=true} 时生效。
 * <p>
 * 【安全注意】本组件不包含任何开发绕过逻辑，所有验证码必须通过正规流程获取。
 */
@Slf4j
@Component
public class LoginCaptchaValidator {

    private static final String CAPTCHA_PREFIX = "captcha:";

    private final SysConfigApplicationService configService;
    private final RedisTemplate<String, String> redisTemplate;

    /** 是否启用验证码校验（可配置，dev 环境可关闭以方便本地调试） */
    @Value("${nexus.captcha.enabled:#{null}}")
    private Boolean captchaEnabledConfig;

    public LoginCaptchaValidator(SysConfigApplicationService configService,
                                 RedisTemplate<String, String> redisTemplate) {
        this.configService = configService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验验证码。
     *
     * @param tenantId     租户ID
     * @param captchaKey   验证码在 Redis 中的 key（由前端生成并传入）
     * @param captchaInput 用户输入的验证码
     * @throws BusinessException 验证码错误、已过期或未开启校验
     */
    public void validate(Long tenantId, String captchaKey, String captchaInput) {
        // 1. 检查是否启用验证码（优先使用配置文件覆盖，否则从数据库配置读取）
        boolean enabled = resolveEnabled(tenantId);
        if (!enabled) {
            log.debug("验证码校验已禁用，tenantId={}", tenantId);
            return;
        }

        // 2. 非空校验
        if (!StringUtils.hasText(captchaInput)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入验证码");
        }

        String trimmed = captchaInput.trim().toUpperCase();

        // 3. Redis 中查找验证码
        if (StringUtils.hasText(captchaKey)) {
            String redisKey = CAPTCHA_PREFIX + captchaKey;
            String expected = redisTemplate.opsForValue().get(redisKey);

            if (StringUtils.hasText(expected) && expected.equals(trimmed)) {
                // 验证成功后删除验证码（一次性使用）
                redisTemplate.delete(redisKey);
                log.debug("验证码校验成功，已删除 key={}", redisKey);
                return;
            }
        }

        // 4. 验证码不匹配 — 删除以防暴力破解
        if (StringUtils.hasText(captchaKey)) {
            redisTemplate.delete(CAPTCHA_PREFIX + captchaKey);
        }
        log.warn("验证码错误或已过期：captchaKey={}, tenantId={}", captchaKey, tenantId);
        throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
    }

    /**
     * 判断验证码校验是否启用。
     * 优先级：配置文件 > 数据库配置 > 默认 true（fail-closed，数据库异常时保持安全）
     */
    private boolean resolveEnabled(Long tenantId) {
        // 显式配置优先
        if (captchaEnabledConfig != null) {
            return captchaEnabledConfig;
        }
        // 从数据库配置读取
        try {
            return configService.getBoolConfig(tenantId, "sys.account.captchaEnabled");
        } catch (Exception e) {
            log.warn("读取验证码配置失败，默认开启校验（fail-closed），tenantId={}", tenantId, e);
            return true;
        }
    }
}