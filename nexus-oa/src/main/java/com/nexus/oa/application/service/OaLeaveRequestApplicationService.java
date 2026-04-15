package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.domain.model.OaLeaveRequest;
import com.nexus.oa.infrastructure.mapper.OaLeaveRequestMapper;
import com.nexus.oa.workflow.LeaveApproverResolver;
import com.nexus.oa.workflow.LeaveRequestStateMachine;
import com.nexus.oa.workflow.OaLeaveRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class OaLeaveRequestApplicationService {

    private final OaLeaveRequestMapper leaveRequestMapper;
    private final LeaveApproverResolver leaveApproverResolver;

    public OaLeaveRequestApplicationService(OaLeaveRequestMapper leaveRequestMapper,
                                           LeaveApproverResolver leaveApproverResolver) {
        this.leaveRequestMapper = leaveRequestMapper;
        this.leaveApproverResolver = leaveApproverResolver;
    }

    public IPage<OaLeaveRequest> page(long current, long size, Integer status) {
        Long tenantId = requireTenantId();
        Page<OaLeaveRequest> p = new Page<>(current, size);
        LambdaQueryWrapper<OaLeaveRequest> w = new LambdaQueryWrapper<OaLeaveRequest>()
                .eq(OaLeaveRequest::getTenantId, tenantId)
                .eq(OaLeaveRequest::getDelFlag, 0)
                .eq(status != null, OaLeaveRequest::getStatus, status)
                .orderByDesc(OaLeaveRequest::getId);
        return leaveRequestMapper.selectPage(p, w);
    }

    public OaLeaveRequest getById(Long id) {
        Long tenantId = requireTenantId();
        OaLeaveRequest row = leaveRequestMapper.selectById(id);
        if (row == null || !Objects.equals(row.getTenantId(), tenantId) || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假单不存在");
        }
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(OaDtos.LeaveRequestCreateRequest req) {
        Long tenantId = requireTenantId();
        Long uid = requireUserId();
        OaLeaveRequest e = new OaLeaveRequest();
        e.setTenantId(tenantId);
        e.setApplicantUserId(uid);
        fillFromCreate(e, req);
        e.setStatus(OaLeaveRequestStatus.DRAFT.getCode());
        e.setApproverUserId(null);
        leaveRequestMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OaDtos.LeaveRequestUpdateRequest req) {
        OaLeaveRequest exist = getById(id);
        assertDraft(exist);
        assertOwner(exist);
        fillFromUpdate(exist, req);
        leaveRequestMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OaLeaveRequest exist = getById(id);
        assertDraft(exist);
        assertOwner(exist);
        leaveRequestMapper.deleteById(exist.getId());
    }

    /**
     * 提交：草稿 → 待审批，并解析审批人（{@link LeaveApproverResolver}）。
     * 仅草稿状态可提交，防止重复提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        Long tenantId = requireTenantId();
        OaLeaveRequest row = getById(id);
        assertOwner(row);
        // 防止重复提交：仅草稿状态可提交
        if (!Objects.equals(row.getStatus(), OaLeaveRequestStatus.DRAFT.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交审批");
        }
        OaLeaveRequestStatus next = LeaveRequestStateMachine.afterSubmit(OaLeaveRequestStatus.fromCode(row.getStatus()));
        Long approver = leaveApproverResolver.resolveApproverUserId(tenantId, row.getApplicantUserId());
        row.setStatus(next.getCode());
        row.setApproverUserId(approver);
        leaveRequestMapper.updateById(row);
    }

    /**
     * 审批：待审批 → 已通过/已驳回（仅当前审批人可操作）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, OaDtos.LeaveApproveRequest req) {
        Long operatorId = requireUserId();
        OaLeaveRequest row = getById(id);
        if (!Objects.equals(row.getApproverUserId(), operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非当前审批人");
        }
        OaLeaveRequestStatus next = LeaveRequestStateMachine.afterDecision(
                OaLeaveRequestStatus.fromCode(row.getStatus()),
                Boolean.TRUE.equals(req.getApproved()));
        row.setStatus(next.getCode());
        leaveRequestMapper.updateById(row);
    }

    private void assertDraft(OaLeaveRequest row) {
        if (!Objects.equals(row.getStatus(), OaLeaveRequestStatus.DRAFT.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿可编辑或删除");
        }
    }

    private void assertOwner(OaLeaveRequest row) {
        Long uid = requireUserId();
        if (!Objects.equals(row.getApplicantUserId(), uid)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅申请人本人可操作");
        }
    }

    private static void fillFromCreate(OaLeaveRequest e, OaDtos.LeaveRequestCreateRequest req) {
        e.setLeaveType(req.getLeaveType().trim());
        e.setStartTime(req.getStartTime());
        e.setEndTime(req.getEndTime());
        e.setLeaveDays(req.getLeaveDays());
        e.setReason(req.getReason());
    }

    private static void fillFromUpdate(OaLeaveRequest e, OaDtos.LeaveRequestUpdateRequest req) {
        e.setLeaveType(req.getLeaveType().trim());
        e.setStartTime(req.getStartTime());
        e.setEndTime(req.getEndTime());
        e.setLeaveDays(req.getLeaveDays());
        e.setReason(req.getReason());
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }

    private static Long requireUserId() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
