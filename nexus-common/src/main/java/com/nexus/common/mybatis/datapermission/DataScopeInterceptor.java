package com.nexus.common.mybatis.datapermission;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.nexus.common.context.DataScopeContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 数据权限：按 {@link DataScopeContext} 的 dataScope（1 全部、2 自定义、3 本部门、4 本部门及以下、5 仅本人）改写 SELECT。
 */
@Slf4j
@Component
public class DataScopeInterceptor implements InnerInterceptor {

    /** 部门/组织隔离列：与任务「dept」对应，库表多为 org_id；sys_user 为 main_org_id。 */
    private static final String COL_ORG = "org_id";
    private static final String COL_MAIN_ORG = "main_org_id";

    private static final Set<String> IGNORE_TABLES = Set.of(
            "flyway_schema_history",
            "sys_dict_type",
            "sys_dict_item",
            "sys_config",
            "sys_menu",
            "sys_role_menu",
            "sys_oper_log",
            "sys_login_log",
            // ERP 表：仅租户隔离，无 org_id 列
            "erp_warehouse",
            "erp_product_info",
            "erp_product_category",
            "erp_supplier",
            "erp_customer",
            "erp_stock",
            "erp_sale_order",
            "erp_sale_order_item",
            "erp_purchase_order",
            "erp_purchase_order_item",
            // 关联表：仅租户隔离，无 org_id 列
            "sys_user_shop_role",
            // 通知与消息表：仅租户隔离，无 org_id 列
            "sys_notice",
            "sys_message",
            // 系统核心表：无 org_id 列（sys_user 用 main_org_id，不是 org_id）
            "sys_org",
            "sys_user",
            "sys_post",
            "sys_user_post",
            "sys_role",
            "sys_shop",
            // OA 审批表：无 org_id 列
            "oa_leave_approval",
            // 新增 ERP 表：仅租户隔离，无 org_id 列
            "erp_opportunity",
            "erp_contract",
            "erp_contract_item",
            "fin_receivable",
            "fin_receivable_record",
            "fin_payable",
            "fin_payable_record",
            // 新增 OA 表：仅租户隔离，无 org_id 列
            "oa_attendance_rule",
            "oa_attendance_record",
            "oa_leave_detail",
            "oa_overtime",
            "oa_approval_task",
            "oa_task",
            "oa_task_comment",
            "oa_schedule",
            "oa_file_folder",
            "oa_file"
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }
        Integer ds = DataScopeContext.getDataScope();
        if (ds == null || ds == 1) {
            return;
        }

        String sql = boundSql.getSql();
        if (sql == null) {
            return;
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select select && select.getSelectBody() instanceof PlainSelect ps)) {
                return;
            }
            String mainTable = primaryTableName(ps);
            if (mainTable == null || IGNORE_TABLES.contains(mainTable)) {
                return;
            }
            String alias = primaryTableAlias(ps, mainTable);
            if (alias == null) {
                return;
            }

            Expression extra = buildFilter(ds, mainTable, alias);
            if (extra == null) {
                return;
            }
            Expression where = ps.getWhere();
            ps.setWhere(and(where, extra));
            String newSql = stmt.toString();
            if (!Objects.equals(sql, newSql)) {
                PluginUtils.mpBoundSql(boundSql).sql(newSql);
            }
        } catch (JSQLParserException e) {
            log.warn("数据权限 SQL 改写失败，跳过: {}", e.getMessage());
        }
    }

    private static Expression buildFilter(int ds, String mainTable, String alias) throws JSQLParserException {
        Long userId = DataScopeContext.getUserId();
        Long deptId = DataScopeContext.getDeptId();
        Long roleId = DataScopeContext.getRoleId();

        if (ds == 5) { // 仅本人数据
            if (userId == null) {
                // 如果没有用户信息但要求隔离，返回 1=0 (用 1=0 阻塞)
                return CCJSqlParserUtil.parseCondExpression("1 = 0");
            }
            return CCJSqlParserUtil.parseCondExpression(alias + ".create_by = " + userId);
        }

        String colName = "sys_user".equals(mainTable) ? COL_MAIN_ORG : COL_ORG;

        if (ds == 3) { // 本部门数据
            if (deptId == null) {
                return CCJSqlParserUtil.parseCondExpression("1 = 0");
            }
            return CCJSqlParserUtil.parseCondExpression(alias + "." + colName + " = " + deptId);
        }

        if (ds == 4) { // 本部门及以下数据
            if (deptId == null) {
                return CCJSqlParserUtil.parseCondExpression("1 = 0");
            }
            // 若依经典实现：使用 id = deptId OR FIND_IN_SET(deptId, ancestors)
            String cond = String.format("%s.%s IN ( SELECT id FROM sys_org WHERE id = %d OR FIND_IN_SET(%d, ancestors) )", alias, colName, deptId, deptId);
            return CCJSqlParserUtil.parseCondExpression(cond);
        }

        if (ds == 2) { // 自定义数据权限
            if (roleId == null) {
                return CCJSqlParserUtil.parseCondExpression("1 = 0");
            }
            // 若依经典实现：使用 sys_role_org 中间表关联
            String cond = String.format("%s.%s IN ( SELECT org_id FROM sys_role_org WHERE role_id = %d )", alias, colName, roleId);
            return CCJSqlParserUtil.parseCondExpression(cond);
        }

        return null;
    }

    private static String primaryTableName(PlainSelect ps) {
        return tableNameOf(ps.getFromItem());
    }

    private static String primaryTableAlias(PlainSelect ps, String mainTable) {
        return tableAliasOf(ps.getFromItem(), mainTable);
    }

    private static String tableNameOf(FromItem from) {
        if (from == null) {
            return null;
        }
        if (from instanceof ParenthesedSelect) {
            return null;
        }
        if (from instanceof ParenthesedFromItem pfi) {
            return tableNameOf(pfi.getFromItem());
        }
        if (from instanceof Table t) {
            return strip(t.getName());
        }
        return null;
    }

    private static String tableAliasOf(FromItem from, String mainTable) {
        if (from == null) {
            return null;
        }
        if (from instanceof ParenthesedSelect) {
            return null;
        }
        if (from instanceof ParenthesedFromItem pfi) {
            return tableAliasOf(pfi.getFromItem(), mainTable);
        }
        if (from instanceof Table t) {
            String name = strip(t.getName());
            if (!mainTable.equals(name)) {
                return null;
            }
            if (t.getAlias() != null && t.getAlias().getName() != null) {
                return strip(t.getAlias().getName());
            }
            return name;
        }
        return null;
    }

    private static String strip(String name) {
        if (name == null) {
            return null;
        }
        return name.replace("`", "").toLowerCase(Locale.ROOT);
    }

    private static Expression and(Expression left, Expression right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return new AndExpression(new Parenthesis(left), new Parenthesis(right));
    }
}
