package com.nexus.wage.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.wage.domain.model.WageMonthlySlip;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WageMonthlySlipMapper extends BaseMapper<WageMonthlySlip> {
}
