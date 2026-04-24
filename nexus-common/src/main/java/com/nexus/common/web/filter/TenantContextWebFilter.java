package com.nexus.common.web.filter;

import com.nexus.common.context.DataScopeContext;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.NexusRequestHeaders;
import com.nexus.common.context.OrgContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.web.auth.InternalAuthInterceptor;
import com.nexus.common.tenant.NexusTenantProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TenantContextWebFilter extends OncePerRequestFilter {

    private final NexusTenantProperties tenantProperties;

    public TenantContextWebFilter(NexusTenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long existingTenantId = TenantContext.getTenantId();
            if (existingTenantId == null) {
                Long tenantId = parseLongHeader(request, NexusRequestHeaders.TENANT_ID);
                if (tenantId == null) {
                    tenantId = tenantProperties.getDefaultTenantId();
                }
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
            }

            if (OrgContext.getShopId() == null) {
                Long shopId = parseLongHeader(request, NexusRequestHeaders.SHOP_ID);
                if (shopId == null) {
                    shopId = tenantProperties.getDefaultShopId();
                }
                if (shopId != null) {
                    OrgContext.setShopId(shopId);
                }
            }

            if (OrgContext.getOrgId() == null) {
                Long orgId = parseLongHeader(request, NexusRequestHeaders.ORG_ID);
                if (orgId == null) {
                    orgId = tenantProperties.getDefaultOrgId();
                }
                if (orgId != null) {
                    OrgContext.setOrgId(orgId);
                }
            }

            // 身份上下文（userId、dataScope、accessibleIds 等）只在 JWT 层已设定后才信任 header，
            // 防止直连下游时伪造身份或权限。
            boolean trustedContext = TenantContext.getTenantId() != null
                    && GatewayUserContext.getUserId() != null;

            if (trustedContext) {
                if (DataScopeContext.getDataScope() == null) {
                    Integer dataScope = parseIntHeader(request, NexusRequestHeaders.DATA_SCOPE);
                    if (dataScope != null) {
                        DataScopeContext.setDataScope(dataScope);
                    }
                }

                if (OrgContext.getAccessibleShopIds().isEmpty()) {
                    List<Long> accessibleShops = parseLongListHeader(request, NexusRequestHeaders.ACCESSIBLE_SHOP_IDS);
                    if (!accessibleShops.isEmpty()) {
                        OrgContext.setAccessibleShopIds(accessibleShops);
                    }
                }

                if (OrgContext.getAccessibleOrgIds().isEmpty()) {
                    List<Long> accessibleOrgs = parseLongListHeader(request, NexusRequestHeaders.ACCESSIBLE_ORG_IDS);
                    if (!accessibleOrgs.isEmpty()) {
                        OrgContext.setAccessibleOrgIds(accessibleOrgs);
                    }
                }

                Long userIdHdr = parseLongHeader(request, InternalAuthInterceptor.HEADER_USER_ID);
                if (userIdHdr != null) {
                    DataScopeContext.setUserId(userIdHdr);
                }
            }
            Long currentOrgId = OrgContext.getOrgId();
            if (currentOrgId != null) {
                DataScopeContext.setDeptId(currentOrgId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.remove();
            OrgContext.clear();
            GatewayUserContext.remove();
            DataScopeContext.clear();
        }
    }

    private static Long parseLongHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseIntHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<Long> parseLongListHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> out = new ArrayList<>();
        for (String part : v.split("[,;\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                out.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
