package com.nexus.system.application.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    @Data
    public static class LoginRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String username;
        private String password;
        /** 与 X-Tenant-Id / 前端 tenantId 对齐，多租户下用于唯一定位账号 */
        private Long tenantId;
        /** 验证码（开启 sys.account.captchaEnabled 时必填，需通过正规渠道获取） */
        private String captcha;
        /** 与 Redis captcha:{captchaKey} 对应 */
        private String captchaKey;
    }

    @Data
    public static class SwitchShopRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long shopId;
    }

    @Data
    public static class ShopItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long shopId;
        private String shopName;
        private Integer shopType;
        private Long orgId;
    }

    @Data
    public static class LoginResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String accessToken;
        private String tokenType;
        private Long tenantId;
        private Long currentShopId;
        private Long currentOrgId;
        private Integer dataScope;
        private List<Long> accessibleShopIds;
        private List<Long> accessibleOrgIds;
    }

}

