package com.nexus.oa.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.oa.domain.model.OaSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OaScheduleMapper extends BaseMapper<OaSchedule> {
}
