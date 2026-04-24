package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.domain.model.*;
import com.nexus.oa.infrastructure.mapper.OaApprovalTaskMapper;
import com.nexus.oa.infrastructure.mapper.OaLeaveDetailMapper;
import com.nexus.oa.infrastructure.mapper.OaOvertimeMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaApprovalCenterService {

    private static final Logger log = LoggerFactory.getLogger(OaApprovalCenterService.class);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final OaApprovalTaskMapper taskMapper;
    private final OaLeaveDetailMapper leaveMapper;
    private final OaOvertimeMapper overtimeMapper;

    // ===================== 审批任务列表 =====================

    /**
     * 我发起的审批
     */
    public IPage<OaApprovalTaskVO> pageMyApply(long current, long size, Integer status) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        Page<OaApprovalTask> p = new Page<>(current, size);
        LambdaQueryWrapper<OaApprovalTask> w = new LambdaQueryWrapper<OaApprovalTask>()
                .eq(OaApprovalTask::getTenantId, tenantId)
                .eq(OaApprovalTask::getDelFlag, 0)
                .eq(OaApprovalTask::getApplicantUserId, userId)
                .eq(status != null, OaApprovalTask::getStatus, status)
                .orderByDesc(OaApprovalTask::getId);
        return taskMapper.selectPage(p, w).convert(this::toTaskVO);
    }

    /**
     * 待我审批
     */
    public IPage<OaApprovalTaskVO> pageMyApprove(long current, long size, Integer status) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        Page<OaApprovalTask> p = new Page<>(current, size);
        LambdaQueryWrapper<OaApprovalTask> w = new LambdaQueryWrapper<OaApprovalTask>()
                .eq(OaApprovalTask::getTenantId, tenantId)
                .eq(OaApprovalTask::getDelFlag, 0)
                .eq(OaApprovalTask::getApproverUserId, userId)
                .eq(status != null, OaApprovalTask::getStatus, status)
                .orderByDesc(OaApprovalTask::getId);
        return taskMapper.selectPage(p, w).convert(this::toTaskVO);
    }

    public OaApprovalTaskVO getById(Long id) {
        Long tenantId = requireTenantId();
        OaApprovalTask t = loadTask(id, tenantId);
        return toTaskVO(t);
    }

    // ===================== 审批 =====================

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, Boolean approved, String opinion) {
        Long tenantId = requireTenantId();
        Long approverId = requireUserId();
        OaApprovalTask task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getTenantId(), tenantId)
                || (task.getDelFlag() != null && task.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }

        // 归属权校验：防止水平越权（IDOR），禁止非任务指派人执行审批动作
        if (!Objects.equals(task.getApproverUserId(), approverId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "越权操作：您无权审批该任务");
        }
        if (task.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该任务已审批，请勿重复操作");
        }

        task.setStatus(approved ? STATUS_APPROVED : STATUS_REJECTED);
        task.setOpinion(opinion);
        task.setApproveTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 同步更新业务单据状态
        syncBizStatus(tenantId, task.getBizType(), task.getBizId(), approved);
    }

    // ===================== 提交审批（由业务模块调用） =====================

    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(String bizType, Long bizId, String title,
                                   String contentSummary, Long applicantUserId,
                                   String applicantUserName, Long approverUserId,
                                   String approverUserName) {
        Long tenantId = requireTenantId();

        // 检查是否已存在审批任务，防止重复提交
        OaApprovalTask existing = taskMapper.selectOne(new LambdaQueryWrapper<OaApprovalTask>()
                .eq(OaApprovalTask::getTenantId, tenantId)
                .eq(OaApprovalTask::getBizType, bizType)
                .eq(OaApprovalTask::getBizId, bizId)
                .eq(OaApprovalTask::getDelFlag, 0));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该单据已提交审批，请勿重复提交");
        }

        OaApprovalTask task = new OaApprovalTask();
        task.setTenantId(tenantId);
        task.setTaskNo(nextNo("AT"));
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setTitle(title);
        task.setContentSummary(contentSummary);
        task.setApplicantUserId(applicantUserId);
        task.setApplicantUserName(applicantUserName);
        task.setApproverUserId(approverUserId);
        task.setApproverUserName(approverUserName);
        task.setStatus(STATUS_PENDING);
        taskMapper.insert(task);
    }

    // ===================== 同步业务单据状态 =====================

    private void syncBizStatus(Long tenantId, String bizType, Long bizId, Boolean approved) {
        if ("leave".equals(bizType)) {
            OaLeave l = leaveMapper.selectById(bizId);
            if (l != null) {
                if (!Objects.equals(l.getTenantId(), tenantId)) {
                    log.warn("审批中心同步业务状态被跳过：跨租户 leave 单据 [bizId={}, expectedTenantId={}, actualTenantId={}]",
                            bizId, tenantId, l.getTenantId());
                    return;
                }
                l.setStatus(approved ? 2 : 3); // 2=已通过 3=已拒绝
                l.setApproverTime(LocalDateTime.now());
                leaveMapper.updateById(l);
            }
        } else if ("overtime".equals(bizType)) {
            OaOvertime o = overtimeMapper.selectById(bizId);
            if (o != null) {
                if (!Objects.equals(o.getTenantId(), tenantId)) {
                    log.warn("审批中心同步业务状态被跳过：跨租户 overtime 单据 [bizId={}, expectedTenantId={}, actualTenantId={}]",
                            bizId, tenantId, o.getTenantId());
                    return;
                }
                o.setStatus(approved ? 2 : 3);
                o.setApproverTime(LocalDateTime.now());
                overtimeMapper.updateById(o);
            }
        }
    }

    // ===================== 私有方法 =====================

    private OaApprovalTask loadTask(Long id, Long tenantId) {
        OaApprovalTask t = taskMapper.selectById(id);
        if (t == null || !Objects.equals(t.getTenantId(), tenantId)
                || (t.getDelFlag() != null && t.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "审批任务不存在");
        }
        return t;
    }

    private OaApprovalTaskVO toTaskVO(OaApprovalTask t) {
        OaApprovalTaskVO vo = new OaApprovalTaskVO();
        vo.setId(t.getId());
        vo.setTaskNo(t.getTaskNo());
        vo.setBizType(t.getBizType());
        vo.setBizId(t.getBizId());
        vo.setTitle(t.getTitle());
        vo.setContentSummary(t.getContentSummary());
        vo.setApplicantUserId(t.getApplicantUserId());
        vo.setApplicantUserName(t.getApplicantUserName());
        vo.setApproverUserId(t.getApproverUserId());
        vo.setApproverUserName(t.getApproverUserName());
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(t.getStatus() == null ? "未知"
                : t.getStatus() == STATUS_PENDING ? "待审批"
                : t.getStatus() == STATUS_APPROVED ? "已通过" : "已拒绝");
        vo.setOpinion(t.getOpinion());
        vo.setApproveTime(t.getApproveTime() != null
                ? t.getApproveTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        vo.setCreateTime(t.getCreateTime() != null
                ? t.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return vo;
    }

    private static String nextNo(String prefix) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(28);
        return prefix + ts + uuidPart;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        return tid;
    }

    private static Long requireUserId() {
        Long uid = SecurityUtils.currentUserId();
        if (uid == null) throw new BusinessException(ResultCode.BAD_REQUEST, "缺少用户上下文");
        return uid;
    }

    // ===================== VO =====================

    @lombok.Data
    public static class OaApprovalTaskVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String taskNo;
        private String bizType;
        private Long bizId;
        private String title;
        private String contentSummary;
        private Long applicantUserId;
        private String applicantUserName;
        private Long approverUserId;
        private String approverUserName;
        private Integer status;
        private String statusLabel;
        private String opinion;
        private String approveTime;
        private String createTime;
    }
}
