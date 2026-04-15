package com.nexus.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexus.system.domain.model.OaLeave;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaLeaveMapper extends BaseMapper<OaLeave> {

    /**
     * 待我审批：与待办审批行内连接，按请假单创建时间倒序分页。
     */
    @Select("SELECT l.* FROM oa_leave l "
            + "INNER JOIN oa_leave_approval a ON l.id = a.leave_id AND l.tenant_id = a.tenant_id "
            + "WHERE l.tenant_id = #{tenantId} AND l.del_flag = 0 "
            + "AND a.approver_user_id = #{userId} AND a.status = 0 AND a.del_flag = 0 "
            + "ORDER BY l.create_time DESC")
    IPage<OaLeave> pagePendingApprove(IPage<OaLeave> page,
                                      @Param("tenantId") Long tenantId,
                                      @Param("userId") Long userId);

    /**
     * 我已审批：按「每单最近一次审批时间」倒序，避免同一请假单多条记录撑爆分页。
     */
    @Select("SELECT l.* FROM oa_leave l "
            + "INNER JOIN ( "
            + "  SELECT leave_id, MAX(approve_time) AS mt FROM oa_leave_approval "
            + "  WHERE tenant_id = #{tenantId} AND approver_user_id = #{userId} AND status IN (1, 2) AND del_flag = 0 "
            + "  GROUP BY leave_id "
            + ") t ON l.id = t.leave_id "
            + "WHERE l.tenant_id = #{tenantId} AND l.del_flag = 0 "
            + "ORDER BY t.mt DESC")
    IPage<OaLeave> pageDoneApprove(IPage<OaLeave> page,
                                   @Param("tenantId") Long tenantId,
                                   @Param("userId") Long userId);
}
