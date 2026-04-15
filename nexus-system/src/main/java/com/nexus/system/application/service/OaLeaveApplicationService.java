package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.application.dto.OaLeaveDtos;
import com.nexus.system.domain.model.OaLeave;
import com.nexus.system.domain.model.OaLeaveApproval;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.infrastructure.mapper.OaLeaveApprovalMapper;
import com.nexus.system.infrastructure.mapper.OaLeaveMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 极简请假审批（纯状态机 + {@link SysOrgApplicationService#findUserIdByPostUpward}）。
 */
@Service
public class OaLeaveApplicationService {

    private static final int LEAVE_STATUS_DEPT = 1;
    private static final int LEAVE_STATUS_GM = 2;
    private static final int LEAVE_STATUS_PASSED = 3;
    private static final int LEAVE_STATUS_REJECTED = 4;

    private static final int APPROVAL_PENDING = 0;
    private static final int APPROVAL_AGREED = 1;
    private static final int APPROVAL_REJECTED = 2;

    private static final String POST_DEPT_MANAGER = "DEPT_MANAGER";
    private static final String POST_GM = "GM";

    private static final BigDecimal THREE_DAYS = new BigDecimal("3");

    private final OaLeaveMapper leaveMapper;
    private final OaLeaveApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final SysOrgApplicationService sysOrgApplicationService;
    private final OaLeaveDictValidator oaLeaveDictValidator;
    private final SysMessageApplicationService sysMessageApplicationService;

    public OaLeaveApplicationService(OaLeaveMapper leaveMapper,
                                     OaLeaveApprovalMapper approvalMapper,
                                     SysUserMapper userMapper,
                                     SysOrgApplicationService sysOrgApplicationService,
                                     OaLeaveDictValidator oaLeaveDictValidator,
                                     SysMessageApplicationService sysMessageApplicationService) {
        this.leaveMapper = leaveMapper;
        this.approvalMapper = approvalMapper;
        this.userMapper = userMapper;
        this.sysOrgApplicationService = sysOrgApplicationService;
        this.oaLeaveDictValidator = oaLeaveDictValidator;
        this.sysMessageApplicationService = sysMessageApplicationService;
    }

    private static final String TYPE_MY_APPLY = "my_apply";
    private static final String TYPE_PENDING_APPROVE = "pending_approve";
    private static final String TYPE_DONE_APPROVE = "done_approve";

    /**
     * 请假单分页：{@code my_apply} 我的申请；{@code pending_approve} 待我审批；{@code done_approve} 我已审批（按最近审批时间）。
     */
    public IPage<OaLeave> page(String type, long pageNum, long pageSize) {
        Long tenantId = requireTenantId();
        Long userId = requireCurrentUserId();
        Page<OaLeave> page = new Page<>(pageNum, pageSize);
        String t = type == null ? TYPE_MY_APPLY : type.trim();
        return switch (t) {
            case TYPE_MY_APPLY -> leaveMapper.selectPage(page, new LambdaQueryWrapper<OaLeave>()
                    .eq(OaLeave::getTenantId, tenantId)
                    .eq(OaLeave::getUserId, userId)
                    .eq(OaLeave::getDelFlag, 0)
                    .orderByDesc(OaLeave::getCreateTime));
            case TYPE_PENDING_APPROVE -> leaveMapper.pagePendingApprove(page, tenantId, userId);
            case TYPE_DONE_APPROVE -> leaveMapper.pageDoneApprove(page, tenantId, userId);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "type 无效，可选：my_apply、pending_approve、done_approve");
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submit(OaLeaveDtos.SubmitRequest req) {
        Long tenantId = requireTenantId();
        Long userId = requireCurrentUserId();
        SysUser applicant = userMapper.selectById(userId);
        if (applicant == null || !Objects.equals(applicant.getTenantId(), tenantId)
                || (applicant.getDelFlag() != null && applicant.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long orgId = applicant.getMainOrgId();
        if (orgId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未设置主属组织，无法提交请假");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
        oaLeaveDictValidator.validateLeaveType(tenantId, req.getLeaveType());

        OaLeave leave = new OaLeave();
        leave.setTenantId(tenantId);
        leave.setUserId(userId);
        leave.setOrgId(orgId);
        leave.setLeaveType(req.getLeaveType().trim());
        leave.setStartTime(req.getStartTime());
        leave.setEndTime(req.getEndTime());
        leave.setDays(req.getDays());
        leave.setReason(StringUtils.hasText(req.getReason()) ? req.getReason().trim() : null);
        leave.setStatus(LEAVE_STATUS_DEPT);
        leaveMapper.insert(leave);

        Long managerId = sysOrgApplicationService.findUserIdByPostUpward(orgId, POST_DEPT_MANAGER);
        if (managerId != null) {
            insertPendingApproval(leave.getId(), tenantId, managerId, POST_DEPT_MANAGER);
        } else {
            leave.setStatus(LEAVE_STATUS_PASSED);
            leaveMapper.updateById(leave);
        }
        return leave.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(OaLeaveDtos.ApproveRequest req) {
        Long tenantId = requireTenantId();
        Long operatorId = requireCurrentUserId();
        OaLeave leave = leaveMapper.selectById(req.getLeaveId());
        if (leave == null || !Objects.equals(leave.getTenantId(), tenantId)
                || (leave.getDelFlag() != null && leave.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假单不存在");
        }
        if (!Objects.equals(leave.getStatus(), LEAVE_STATUS_DEPT) && !Objects.equals(leave.getStatus(), LEAVE_STATUS_GM)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不可审批");
        }

        OaLeaveApproval pending = approvalMapper.selectOne(new LambdaQueryWrapper<OaLeaveApproval>()
                .eq(OaLeaveApproval::getLeaveId, leave.getId())
                .eq(OaLeaveApproval::getTenantId, tenantId)
                .eq(OaLeaveApproval::getDelFlag, 0)
                .eq(OaLeaveApproval::getStatus, APPROVAL_PENDING)
                .orderByDesc(OaLeaveApproval::getId)
                .last("LIMIT 1"));
        if (pending == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有待处理的审批任务");
        }
        if (!Objects.equals(pending.getApproverUserId(), operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非当前审批人，无法操作");
        }

        oaLeaveDictValidator.validateLeaveType(tenantId, leave.getLeaveType());

        LocalDateTime now = LocalDateTime.now();
        boolean approved = Boolean.TRUE.equals(req.getApproved());
        pending.setStatus(approved ? APPROVAL_AGREED : APPROVAL_REJECTED);
        pending.setOpinion(StringUtils.hasText(req.getOpinion()) ? req.getOpinion().trim() : null);
        pending.setApproveTime(now);
        approvalMapper.updateById(pending);

        if (!approved) {
            leave.setStatus(LEAVE_STATUS_REJECTED);
            leaveMapper.updateById(leave);
            notifyApplicant(tenantId, leave, req);
            return;
        }

        if (Objects.equals(leave.getStatus(), LEAVE_STATUS_DEPT)) {
            if (leave.getDays() == null || leave.getDays().compareTo(THREE_DAYS) <= 0) {
                leave.setStatus(LEAVE_STATUS_PASSED);
            } else {
                Long gmId = sysOrgApplicationService.findUserIdByPostUpward(leave.getOrgId(), POST_GM);
                if (gmId != null) {
                    insertPendingApproval(leave.getId(), tenantId, gmId, POST_GM);
                    leave.setStatus(LEAVE_STATUS_GM);
                } else {
                    leave.setStatus(LEAVE_STATUS_PASSED);
                }
            }
        } else {
            leave.setStatus(LEAVE_STATUS_PASSED);
        }
        leaveMapper.updateById(leave);
        notifyApplicant(tenantId, leave, req);
    }

    private void notifyApplicant(Long tenantId, OaLeave leave, OaLeaveDtos.ApproveRequest req) {
        String result = Objects.equals(leave.getStatus(), LEAVE_STATUS_REJECTED)
                ? "拒绝"
                : (Objects.equals(leave.getStatus(), LEAVE_STATUS_PASSED) ? "通过（流程结束）" : "通过（进入下一审批）");
        StringBuilder content = new StringBuilder();
        content.append("审批结果：").append(result);
        if (StringUtils.hasText(req.getOpinion())) {
            content.append("；意见：").append(req.getOpinion().trim());
        }
        sysMessageApplicationService.sendToUser(tenantId, leave.getUserId(), "你的请假单已审批",
                content.toString(), SysMessageApplicationService.TYPE_APPROVAL);
    }

    private void insertPendingApproval(Long leaveId, Long tenantId, Long approverUserId, String postCode) {
        OaLeaveApproval row = new OaLeaveApproval();
        row.setTenantId(tenantId);
        row.setLeaveId(leaveId);
        row.setApproverUserId(approverUserId);
        row.setPostCode(postCode);
        row.setStatus(APPROVAL_PENDING);
        approvalMapper.insert(row);
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tenantId;
    }

    private static Long requireCurrentUserId() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
