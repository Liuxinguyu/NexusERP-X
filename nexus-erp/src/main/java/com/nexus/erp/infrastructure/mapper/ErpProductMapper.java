package com.nexus.erp.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.erp.domain.model.ErpProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpProductMapper extends BaseMapper<ErpProduct> {
}
