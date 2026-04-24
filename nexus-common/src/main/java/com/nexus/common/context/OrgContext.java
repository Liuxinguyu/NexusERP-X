package com.nexus.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 请求线程内的组织与店铺维度上下文（树状组织 + 当前店铺），替代已移除的 {@code ShopContext}。
 */
public final class OrgContext {

    private static final ThreadLocal<Long> ORG_ID = new TransmittableThreadLocal<>();
    private static final ThreadLocal<Long> SHOP_ID = new TransmittableThreadLocal<>();
    private static final ThreadLocal<List<Long>> ACCESSIBLE_ORG_IDS = new TransmittableThreadLocal<>();
    private static final ThreadLocal<List<Long>> ACCESSIBLE_SHOP_IDS = new TransmittableThreadLocal<>();

    private OrgContext() {
    }

    public static void setOrgId(Long orgId) {
        if (orgId == null) {
            ORG_ID.remove();
        } else {
            ORG_ID.set(orgId);
        }
    }

    public static Long getOrgId() {
        return ORG_ID.get();
    }

    public static void setShopId(Long shopId) {
        if (shopId == null) {
            SHOP_ID.remove();
        } else {
            SHOP_ID.set(shopId);
        }
    }

    public static Long getShopId() {
        return SHOP_ID.get();
    }

    public static void setAccessibleOrgIds(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            ACCESSIBLE_ORG_IDS.remove();
            return;
        }
        ACCESSIBLE_ORG_IDS.set(Collections.unmodifiableList(new ArrayList<>(orgIds)));
    }

    public static List<Long> getAccessibleOrgIds() {
        List<Long> ids = ACCESSIBLE_ORG_IDS.get();
        return ids == null ? List.of() : ids;
    }

    public static void setAccessibleShopIds(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            ACCESSIBLE_SHOP_IDS.remove();
            return;
        }
        ACCESSIBLE_SHOP_IDS.set(Collections.unmodifiableList(new ArrayList<>(shopIds)));
    }

    public static List<Long> getAccessibleShopIds() {
        List<Long> ids = ACCESSIBLE_SHOP_IDS.get();
        return ids == null ? List.of() : ids;
    }

    public static void clear() {
        ORG_ID.remove();
        SHOP_ID.remove();
        ACCESSIBLE_ORG_IDS.remove();
        ACCESSIBLE_SHOP_IDS.remove();
    }
}
