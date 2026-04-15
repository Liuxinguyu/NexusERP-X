package com.nexus.system.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

public final class SystemOrgDtos {
    private SystemOrgDtos() {
    }

    @Data
    public static class OrgCreateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long parentId;
        @NotBlank
        private String orgCode;
        @NotBlank
        private String orgName;
        private Integer orgType;
        private Integer sort;
        private Integer status;
    }

    @Data
    public static class UserChangeOrgRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long userId;
        @NotNull
        private Long newOrgId;
    }

    @Data
    public static class OrgUpdateRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long id;
        @NotNull
        private Long parentId;
        @NotBlank
        private String orgCode;
        @NotBlank
        private String orgName;
        private Integer orgType;
        private Integer sort;
        private Integer status;
    }
}
