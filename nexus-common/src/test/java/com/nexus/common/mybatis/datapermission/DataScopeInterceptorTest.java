package com.nexus.common.mybatis.datapermission;

import com.nexus.common.context.DataScopeContext;
import com.nexus.common.context.OrgContext;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeInterceptorTest {

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
        OrgContext.clear();
    }

    private static MappedStatement buildMs(Configuration cfg, SqlCommandType type) {
        SqlSource sqlSource = (parameterObject) -> null;
        return new MappedStatement.Builder(cfg, "test.select", sqlSource, type).build();
    }

    @Test
    void selfScopeAppendsCreateBy() throws Exception {
        // DataScope.SELF = 1
        DataScopeContext.setDataScope(1);
        DataScopeContext.setUserId(99L);

        Configuration configuration = new Configuration();
        String original = "SELECT id, name FROM erp_product p WHERE del_flag = 0";
        BoundSql boundSql = new BoundSql(configuration, original, List.of(), null);

        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        MappedStatement ms = buildMs(configuration, SqlCommandType.SELECT);

        interceptor.beforeQuery(null, ms, null, RowBounds.DEFAULT, null, boundSql);

        assertThat(boundSql.getSql().toLowerCase()).matches("(?s).*create_by\\s*=\\s*99.*");
    }

    @Test
    void deptScopeAppendsOrgId() throws Exception {
        // DataScope.ORG_AND_SUB_SHOPS = 3
        DataScopeContext.setDataScope(3);
        DataScopeContext.setDeptId(7L);

        Configuration configuration = new Configuration();
        String original = "SELECT id FROM erp_product t WHERE 1=1";
        BoundSql boundSql = new BoundSql(configuration, original, List.of(), null);

        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        MappedStatement ms = buildMs(configuration, SqlCommandType.SELECT);

        interceptor.beforeQuery(null, ms, null, RowBounds.DEFAULT, null, boundSql);

        // ORG_AND_SUB_SHOPS generates: org_id IN (SELECT id FROM sys_org WHERE id = 7 OR FIND_IN_SET(7, ancestors))
        assertThat(boundSql.getSql().toLowerCase()).matches("(?s).*org_id\\s+in\\s*\\(.*select.*sys_org.*7.*\\).*");
    }

    @Test
    void shopScopeAppendsAccessibleShopIds() throws Exception {
        // DataScope.SHOP = 2
        DataScopeContext.setDataScope(2);
        OrgContext.setAccessibleShopIds(List.of(10L, 20L));

        Configuration configuration = new Configuration();
        String original = "SELECT id FROM erp_product t WHERE 1=1";
        BoundSql boundSql = new BoundSql(configuration, original, List.of(), null);

        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        MappedStatement ms = buildMs(configuration, SqlCommandType.SELECT);

        interceptor.beforeQuery(null, ms, null, RowBounds.DEFAULT, null, boundSql);

        assertThat(boundSql.getSql().toLowerCase()).matches("(?s).*org_id\\s+in\\s*\\(\\s*10\\s*,\\s*20\\s*\\).*");
    }
}
