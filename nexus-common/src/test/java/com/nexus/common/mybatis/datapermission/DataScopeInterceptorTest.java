package com.nexus.common.mybatis.datapermission;

import com.nexus.common.context.DataScopeContext;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataScopeInterceptorTest {

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    @Test
    void selfScopeAppendsCreateBy() throws Exception {
        DataScopeContext.setDataScope(5);
        DataScopeContext.setUserId(99L);

        Configuration configuration = new Configuration();
        String original = "SELECT id, name FROM erp_product p WHERE del_flag = 0";
        BoundSql boundSql = new BoundSql(configuration, original, List.of(), null);

        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        MappedStatement ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);

        interceptor.beforeQuery(null, ms, null, RowBounds.DEFAULT, null, boundSql);

        assertThat(boundSql.getSql().toLowerCase()).matches("(?s).*create_by\\s*=\\s*99.*");
    }

    @Test
    void deptScopeAppendsOrgId() throws Exception {
        DataScopeContext.setDataScope(3);
        DataScopeContext.setDeptId(7L);

        Configuration configuration = new Configuration();
        String original = "SELECT id FROM erp_product t WHERE 1=1";
        BoundSql boundSql = new BoundSql(configuration, original, List.of(), null);

        DataScopeInterceptor interceptor = new DataScopeInterceptor();
        MappedStatement ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);

        interceptor.beforeQuery(null, ms, null, RowBounds.DEFAULT, null, boundSql);

        assertThat(boundSql.getSql().toLowerCase()).matches("(?s).*org_id\\s*=\\s*7.*");
    }
}
