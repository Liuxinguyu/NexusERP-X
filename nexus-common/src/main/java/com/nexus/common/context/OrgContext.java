package com.nexus.common.context;

import com.nexus.common.security.datascope.DataScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 请求线程内的组织与店铺维度上下文（树状组织 + 当前店铺），替代已移除的 {@code ShopContext}。
 */
public final class OrgContext {

    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> SHOP_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> DATA_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<List<Long>> ACCESSIBLE_ORG_IDS = new ThreadLocal<>();
    private static final ThreadLocal<List<Long>> ACCESSIBLE_SHOP_IDS = new ThreadLocal<>();

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

    public static void setDataScope(Integer dataScope) {
        if (dataScope == null) {
            DATA_SCOPE.remove();
        } else {
            DATA_SCOPE.set(dataScope);
        }
    }

    public static Integer getDataScope() {
        return DATA_SCOPE.get();
    }

    public static DataScope getDataScopeEnum() {
        return DataScope.fromCode(getDataScope());
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
        DATA_SCOPE.remove();
        ACCESSIBLE_ORG_IDS.remove();
        ACCESSIBLE_SHOP_IDS.remove();
    }
}
