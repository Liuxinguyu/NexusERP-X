package com.nexus.oa.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.oa.domain.model.OaEmployee;
import com.nexus.oa.infrastructure.mapper.OaEmployeeMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于员工档案的审批人解析：申请人需存在 {@link OaEmployee#userId} 绑定，且配置 {@link OaEmployee#directLeaderUserId}。
 * （system 模块若在同一 JVM，可另实现委托给 {@code SysOrgApplicationService#findUserIdByPostUpward} 的 Adapter。）
 */
@Component
public class EmployeeBasedLeaveApproverResolver implements LeaveApproverResolver {

    private final OaEmployeeMapper employeeMapper;

    public EmployeeBasedLeaveApproverResolver(OaEmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Long resolveApproverUserId(Long tenantId, Long applicantUserId) {
        OaEmployee emp = employeeMapper.selectOne(new LambdaQueryWrapper<OaEmployee>()
                .eq(OaEmployee::getTenantId, tenantId)
                .eq(OaEmployee::getUserId, applicantUserId)
                .eq(OaEmployee::getDelFlag, 0)
                .last("LIMIT 1"));
        if (emp == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未找到与登录用户绑定的员工档案，无法解析审批人");
        }
        Long leader = emp.getDirectLeaderUserId();
        if (leader == null || Objects.equals(leader, applicantUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "员工档案未配置直属上级审批人(direct_leader_user_id)");
        }
        return leader;
    }
}
