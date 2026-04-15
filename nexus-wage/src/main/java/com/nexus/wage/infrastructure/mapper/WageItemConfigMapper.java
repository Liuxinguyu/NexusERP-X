package com.nexus.wage.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.wage.domain.model.WageItemConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WageItemConfigMapper extends BaseMapper<WageItemConfig> {
}
