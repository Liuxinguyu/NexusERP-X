package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexus.system.domain.model.SysPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {

    /**
     * 在指定主属组织下查找拥有某岗位编码的启用用户（取 id 最小的一条）。
     */
    @Select("SELECT u.id FROM sys_user u "
            + "INNER JOIN sys_user_post up ON u.id = up.user_id AND up.del_flag = 0 AND up.tenant_id = #{tenantId} "
            + "INNER JOIN sys_post p ON up.post_id = p.id AND p.del_flag = 0 AND p.tenant_id = #{tenantId} "
            + "WHERE u.tenant_id = #{tenantId} AND u.del_flag = 0 AND (u.status = 1 OR u.status IS NULL) "
            + "AND u.main_org_id = #{orgId} AND p.post_code = #{postCode} AND (p.status = 1 OR p.status IS NULL) "
            + "ORDER BY u.id ASC LIMIT 1")
    Long selectFirstUserIdByMainOrgAndPost(@Param("tenantId") Long tenantId,
                                           @Param("orgId") Long orgId,
                                           @Param("postCode") String postCode);
}
