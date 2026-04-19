#!/bin/bash
set -e

# Write Dict DTOs
cat << 'INNER_EOF' > /Users/liuxingyu/NexusERP-X/nexus-system/src/main/java/com/nexus/system/application/dto/SystemDictDtos.java
package com.nexus.system.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class SystemDictDtos {

    @Data
    public static class DictTypeCreateRequest {
        @NotBlank(message = "字典名称不能为空")
        private String dictName;
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        private Integer status;
        private String remark;
    }

    @Data
    public static class DictTypeUpdateRequest {
        @NotBlank(message = "字典名称不能为空")
        private String dictName;
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        private Integer status;
        private String remark;
    }

    @Data
    public static class DictItemCreateRequest {
        @NotBlank(message = "字典类型不能为空")
        private String dictType;
        @NotBlank(message = "字典标签不能为空")
        private String label;
        @NotBlank(message = "字典键值不能为空")
        private String value;
        private Integer sort;
        private Integer status;
        private String remark;
        private String cssClass;
        private String listClass;
    }

    @Data
    public static class DictItemUpdateRequest {
        @NotBlank(message = "字典标签不能为空")
        private String label;
        @NotBlank(message = "字典键值不能为空")
        private String value;
        private Integer sort;
        private Integer status;
        private String remark;
        private String cssClass;
        private String listClass;
    }
}
INNER_EOF

