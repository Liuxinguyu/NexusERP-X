package com.nexus.common.mybatis.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NexusTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
            "flyway_schema_history"
    );

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺少租户上下文 tenant_id");
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        return IGNORE_TABLES.contains(tableName.replace("`", "").toLowerCase());
    }
}

