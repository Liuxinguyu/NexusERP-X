package com.nexus.system.application.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public final class UserInfoDtos {
    private UserInfoDtos() {
    }

    @Data
    public static class UserInfoResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private UserProfile profile;
        private List<MenuNode> menus;
        /** 用户拥有的全部权限字符串（含按钮权限），前端 PermGate/can() 依赖此字段 */
        private List<String> permissions;
        /** 用户在当前店铺下的角色标识列表（roleCode），前端 RoleGate/hasRole() 依赖此字段 */
        private List<String> roles;
        /** 当前租户下最新一条已发布且未过期公告标题（首页展示） */
        private String latestNoticeTitle;
    }

    @Data
    public static class UserProfile implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long userId;
        private String username;
        private String realName;
        private String avatarUrl;
        private Long tenantId;
        private Long currentShopId;
        private Long currentOrgId;
        private Integer dataScope;
        private List<Long> accessibleShopIds;
        private List<Long> accessibleOrgIds;
    }

    @Data
    public static class MenuNode implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long parentId;
        private String menuType;
        private String menuName;
        private String path;
        /**
         * 从根拼接的完整路径（如 /system/user），用于菜单 router 索引与动态路由。
         */
        private String fullPath;
        private String component;
        private String icon;
        private String perms;
        private Integer sort;
        private List<MenuNode> children;
    }
}

