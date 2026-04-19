package com.nexus.common.context;

/**
 * 同租户内数据权限（部门/本人）线程上下文，与 JWT / 网关 Header 注入对齐。
 */
public final class DataScopeContext {

    private static final ThreadLocal<Integer> DATA_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ROLE_ID = new ThreadLocal<>();

    private DataScopeContext() {
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

    public static void setDeptId(Long deptId) {
        if (deptId == null) {
            DEPT_ID.remove();
        } else {
            DEPT_ID.set(deptId);
        }
    }

    public static Long getDeptId() {
        return DEPT_ID.get();
    }

    public static void setUserId(Long userId) {
        if (userId == null) {
            USER_ID.remove();
        } else {
            USER_ID.set(userId);
        }
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setRoleId(Long roleId) {
        if (roleId == null) {
            ROLE_ID.remove();
        } else {
            ROLE_ID.set(roleId);
        }
    }

    public static Long getRoleId() {
        return ROLE_ID.get();
    }

    public static void clear() {
        DATA_SCOPE.remove();
        DEPT_ID.remove();
        USER_ID.remove();
        ROLE_ID.remove();
    }
}
