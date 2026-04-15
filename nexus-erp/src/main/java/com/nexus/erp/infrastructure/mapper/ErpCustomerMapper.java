package com.nexus.erp.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.erp.domain.model.ErpCustomer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpCustomerMapper extends BaseMapper<ErpCustomer> {
}
