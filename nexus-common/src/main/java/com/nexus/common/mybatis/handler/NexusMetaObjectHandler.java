package com.nexus.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.OrgContext;
import com.nexus.common.context.TenantContext;
import com.nexus.common.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

public class NexusMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            uid = GatewayUserContext.getUserId();
        }
        if (uid != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, uid);
            this.strictInsertFill(metaObject, "updateBy", Long.class, uid);
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
        }

        Long shopId = OrgContext.getShopId();
        if (shopId != null) {
            this.strictInsertFill(metaObject, "shopId", Long.class, shopId);
        }

        Long orgId = OrgContext.getOrgId();
        if (orgId != null) {
            this.strictInsertFill(metaObject, "orgId", Long.class, orgId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);

        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            uid = GatewayUserContext.getUserId();
        }
        if (uid != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, uid);
        }
    }
}
