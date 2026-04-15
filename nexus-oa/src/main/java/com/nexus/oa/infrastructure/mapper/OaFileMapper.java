package com.nexus.oa.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.oa.domain.model.OaFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OaFileMapper extends BaseMapper<OaFile> {
}
