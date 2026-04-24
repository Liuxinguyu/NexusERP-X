package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.system.domain.model.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户 ID 查询功能权限标识
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param shopId   店铺 ID（可选，传 null 则不限制店铺）
     * @return 权限列表
     */
    List<String> selectMenuPermsByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("shopId") Long shopId);
}
