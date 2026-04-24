package com.nexus.wage.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.nexus.common.domain.model.BaseTenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wage_item_config")
public class WageItemConfig extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 薪资项名称，如基本工资、交通补贴、迟到扣款 */
    private String itemName;

    /** 计算类型：1 固定值 2 手动录入 */
    private Integer calcType;

    /** 默认金额 */
    private BigDecimal defaultAmount;

    /**
     * 计入工资单时的归类：1 基本工资 2 补贴（合计入补贴）3 扣款（合计入扣款）。
     * 一键生成月工资时按此类别汇总到工资单对应栏目。
     */
    private Integer itemKind;

    @Version
    private Integer version;
}
