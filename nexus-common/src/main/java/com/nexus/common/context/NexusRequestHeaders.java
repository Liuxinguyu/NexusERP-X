package com.nexus.common.context;

public final class NexusRequestHeaders {
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String SHOP_ID = "X-Shop-Id";
    public static final String ORG_ID = "X-Org-Id";
    public static final String DATA_SCOPE = "X-Data-Scope";
    public static final String ACCESSIBLE_SHOP_IDS = "X-Accessible-Shop-Ids";
    public static final String ACCESSIBLE_ORG_IDS = "X-Accessible-Org-Ids";

    private NexusRequestHeaders() {
    }
}
