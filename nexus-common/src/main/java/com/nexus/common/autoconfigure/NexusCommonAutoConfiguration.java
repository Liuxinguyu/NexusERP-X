package com.nexus.common.autoconfigure;

import com.nexus.common.security.config.NexusSecurityProperties;
import com.nexus.common.tenant.NexusTenantProperties;
import com.nexus.common.web.filter.TenantContextWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@AutoConfiguration
@ComponentScan(
        basePackages = "com.nexus.common",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NexusCommonAutoConfiguration.class)
)
@EnableConfigurationProperties({NexusTenantProperties.class, NexusSecurityProperties.class})
public class NexusCommonAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public TenantContextWebFilter tenantContextWebFilter(NexusTenantProperties tenantProperties) {
        return new TenantContextWebFilter(tenantProperties);
    }
}

