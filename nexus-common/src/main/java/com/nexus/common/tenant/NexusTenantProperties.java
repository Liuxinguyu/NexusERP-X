package com.nexus.common.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nexus.tenant")
public class NexusTenantProperties {
    private Long defaultTenantId;
    private Long defaultShopId;
    /** 业务表 org_id 默认值（如未传组织） */
    private Long defaultOrgId = 1L;
}

