package com.nexus.common.core.page;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页码，从1开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
