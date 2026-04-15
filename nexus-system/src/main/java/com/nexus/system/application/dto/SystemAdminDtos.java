package com.nexus.system.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public final class SystemAdminDtos {
    private SystemAdminDtos() {
    }

    @Data
    public static class ShopCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long orgId;
        @NotBlank
        private String shopName;
        @NotNull
        private Integer shopType;
        private Integer status;
    }

    @Data
    public static class ShopUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long orgId;
        @NotBlank
        private String shopName;
        @NotNull
        private Integer shopType;
    }

    @Data
    public static class ShopStatusRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Integer status;
    }

    @Data
    public static class RoleCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long shopId;
        @NotBlank
        private String roleCode;
        @NotBlank
        private String roleName;
        @NotNull
        private Integer dataScope;
    }

    @Data
    public static class RoleUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long shopId;
        @NotBlank
        private String roleCode;
        @NotBlank
        private String roleName;
        @NotNull
        private Integer dataScope;
    }

    @Data
    public static class RoleMenuAssignRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private List<Long> menuIds;
    }

    @Data
    public static class UserCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private String realName;
        @NotNull
        private Long mainShopId;
        private Integer status;
    }

    @Data
    public static class UserUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String realName;
        /** 为空则不修改密码 */
        private String password;
        @NotNull
        private Long mainShopId;
        private Integer status;
    }

    @Data
    public static class UserShopRoleItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long shopId;
        @NotNull
        private Long roleId;
    }

    @Data
    public static class UserShopRoleSaveRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private List<UserShopRoleItem> items;
    }

    @Data
    public static class MenuTreeNode implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long parentId;
        private String menuType;
        private String menuName;
        private String path;
        private String component;
        private Integer sort;
        private List<MenuTreeNode> children;
    }

    @Data
    public static class UserShopRoleRow implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long shopId;
        private String shopName;
        private Long roleId;
        private String roleName;
    }

    @Data
    public static class ShopOption implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String shopName;
    }

    @Data
    public static class RoleOption implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String roleName;
        private String roleCode;
        private Long shopId;
        private Integer dataScope;
    }

    @Data
    public static class NoticeSaveRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 编辑时传入，新增时为 null */
        private Long id;
        @NotBlank
        private String title;
        private String content;
        private String noticeType;
        /** 公告过期时间，可为空（永不过期） */
        private LocalDateTime expireTime;
    }
}
