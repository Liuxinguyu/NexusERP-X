package com.nexus.oa.workflow;

/**
 * 解析「当前应由谁审批」——对标 system 侧 {@code SysOrgApplicationService#findUserIdByPostUpward} 的职责，
 * OA 独立部署时使用员工档案上的直属上级用户 ID（参见 {@link com.nexus.oa.domain.model.OaEmployee#getDirectLeaderUserId()}）。
 */
@FunctionalInterface
public interface LeaveApproverResolver {

    /**
     * @param tenantId     租户
     * @param applicantUserId 申请人（登录用户 ID）
     * @return 审批人用户 ID，不可为空
     */
    Long resolveApproverUserId(Long tenantId, Long applicantUserId);
}
