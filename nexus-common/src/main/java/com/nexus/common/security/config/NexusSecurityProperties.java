package com.nexus.common.security.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "nexus.security")
public class NexusSecurityProperties {

    private final Jwt jwt = new Jwt();

    /**
     * 仅用于本地联调：放行所有以 /api/v1/ 开头的接口。生产环境必须设为 false，并依赖 JWT + authenticated()。
     */
    private boolean localDevPermitAllApi = false;

    @Autowired
    private Environment environment;

    private String[] permitAllPatterns = new String[]{
            "/actuator/**",
            "/actuator/health",
            "/actuator/info",
            "/api/v1/auth/login",
            "/api/v1/auth/confirm-shop",
            "/api/v1/system/captcha/**",
            "/error",
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v2/api-docs",
            "/v2/api-docs/**",
            "/webjars/**"
    };

    @PostConstruct
    public void validateJwtSecret() {
        if (localDevPermitAllApi && environment != null) {
            for (String profile : environment.getActiveProfiles()) {
                if (profile.contains("prod")) {
                    throw new IllegalStateException(
                            "nexus.security.local-dev-permit-all-api=true 在生产 profile（" + profile + "）中被禁止");
                }
            }
        }

        if (!StringUtils.hasText(jwt.getSecret())) {
            throw new IllegalStateException(
                    "nexus.security.jwt.secret 必须配置，请检查 application.yml 或环境变量");
        }
        if (jwt.getSecret().length() < 32) {
            throw new IllegalStateException("nexus.security.jwt.secret 长度必须不少于 32 字符");
        }
        // 检测已知的不安全示例密钥
        if (jwt.getSecret().contains("NexusERP") && jwt.getSecret().contains("SecretKey")) {
            throw new IllegalStateException(
                    "检测到不安全的 JWT 密钥（示例值），生产环境必须修改 nexus.security.jwt.secret"
                            + "（可通过环境变量 JWT_SECRET 注入，推荐使用 `openssl rand -base64 32` 生成）");
        }
    }

    @Data
    public static class Jwt {
        /**
         * JWT 签名密钥（必填），生产环境必须通过配置或环境变量覆盖，绝不能使用默认值。
         * 推荐使用 32+ 字符的随机字符串，可通过 `openssl rand -base64 32` 生成。
         * 【安全警告】禁止在代码中硬编码此值，必须通过环境变量或外部配置注入。
         */
        private String secret;

        private long expirationSeconds = 7200L;
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer ";
    }
}