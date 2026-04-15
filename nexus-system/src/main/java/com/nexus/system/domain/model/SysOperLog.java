package com.nexus.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String username;
    private String module;
    private String operType;
    private String operUrl;
    private String operMethod;
    private String operIp;
    private String requestParam;
    private String responseData;
    /** 0 失败 1 成功 */
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime createTime;
}
