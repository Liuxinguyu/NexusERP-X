package com.nexus.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new TransmittableThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(tenantId);
        }
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    public static void remove() {
        clear();
    }
}

