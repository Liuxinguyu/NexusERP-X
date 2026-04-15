package com.nexus.common.security.datascope;

import lombok.Getter;

@Getter
public enum DataScope {
    SELF(1),
    SHOP(2),
    /** 当前组织及其子组织（及关联店铺）范围内的数据 */
    ORG_AND_SUB_SHOPS(3),
    ALL(4);

    private final int code;

    DataScope(int code) {
        this.code = code;
    }

    public static DataScope fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScope v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        return null;
    }
}
