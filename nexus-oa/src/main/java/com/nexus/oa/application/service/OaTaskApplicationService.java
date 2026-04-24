package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.SecurityUtils;
import com.nexus.oa.domain.model.OaTask;
import com.nexus.oa.domain.model.OaTaskComment;
import com.nexus.oa.infrastructure.mapper.OaTaskCommentMapper;
import com.nexus.oa.infrastructure.mapper.OaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaTaskApplicationService {

    private static final int STATUS_PENDING = 0;  // 待接受
    private static final int STATUS_ONGOING = 1;   // 进行中
    private static final int STATUS_DONE = 2;       // 已完成
    private static final int STATUS_CANCELLED = 3;  // 已取消

    private final OaTaskMapper taskMapper;
    private final OaTaskCommentMapper commentMapper;

    public IPage<TaskVO> page(long current, long size, Integer status, Long assigneeId) {
        Long tenantId = requireTenantId();
        Page<OaTask> p = new Page<>(current, size);
        LambdaQueryWrapper<OaTask> w = new LambdaQueryWrapper<OaTask>()
                .eq(OaTask::getTenantId, tenantId)
                .eq(OaTask::getDelFlag, 0)
                .eq(status != null, OaTask::getStatus, status)
                .eq(assigneeId != null, OaTask::getAssigneeUserId, assigneeId)
                .orderByDesc(OaTask::getId);
        return taskMapper.selectPage(p, w).convert(this::toVO);
    }

    public TaskVO getById(Long id) {
        Long tenantId = requireTenantId();
        return toVO(loadTask(id, tenantId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskCreateReq req) {
        Long tenantId = requireTenantId();
        Long creatorId = requireUserId();
        NexusPrincipal principal = SecurityUtils.currentPrincipal();
        String creatorName = principal != null ? principal.getUsername() : "system";

        OaTask t = new OaTask();
        t.setTenantId(tenantId);
        t.setTaskNo(nextNo("TK"));
        t.setTitle(req.getTitle().trim());
        t.setDescription(req.getDescription());
        t.setPriority(req.getPriority() != null ? req.getPriority() : 2);
        t.setStatus(STATUS_PENDING);
        t.setAssigneeUserId(req.getAssigneeUserId());
        t.setAssigneeUserName(req.getAssigneeUserName());
        t.setCreatorUserId(creatorId);
        if (req.getDueDate() != null && !req.getDueDate().isBlank()) {
            t.setDueDate(LocalDate.parse(req.getDueDate()));
        }
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            t.setStartDate(LocalDate.parse(req.getStartDate()));
        }
        t.setProgress(0);
        t.setTags(req.getTags());
        taskMapper.insert(t);
        return t.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TaskUpdateReq req) {
        Long tenantId = requireTenantId();
        OaTask t = loadTask(id, tenantId);
        if (t.getStatus() == STATUS_DONE || t.getStatus() == STATUS_CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已完成或已取消的任务不可修改");
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            t.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getPriority() != null) t.setPriority(req.getPriority());
        if (req.getAssigneeUserId() != null) {
            t.setAssigneeUserId(req.getAssigneeUserId());
            t.setAssigneeUserName(req.getAssigneeUserName());
        }
        if (req.getDueDate() != null && !req.getDueDate().isBlank()) {
            t.setDueDate(LocalDate.parse(req.getDueDate()));
        }
        if (req.getTags() != null) t.setTags(req.getTags());
        taskMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        loadTask(id, tenantId);
        commentMapper.delete(new LambdaQueryWrapper<OaTaskComment>()
                .eq(OaTaskComment::getTaskId, id));
        taskMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaTask t = loadTask(id, tenantId);
        if (!Objects.equals(t.getAssigneeUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有负责人可以接受任务");
        }
        if (t.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有待接受的任务可接受");
        }
        t.setStatus(STATUS_ONGOING);
        taskMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long id, int progress) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaTask t = loadTask(id, tenantId);
        if (!Objects.equals(t.getAssigneeUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有负责人可以更新进度");
        }
        if (t.getStatus() == STATUS_DONE || t.getStatus() == STATUS_CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已完成的任务不可更新");
        }
        t.setProgress(progress);
        if (progress >= 100) {
            t.setStatus(STATUS_DONE);
            t.setCompletedTime(LocalDateTime.now());
        } else if (t.getStatus() == STATUS_PENDING) {
            t.setStatus(STATUS_ONGOING);
        }
        taskMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        Long tenantId = requireTenantId();
        OaTask t = loadTask(id, tenantId);
        if (t.getStatus() == STATUS_DONE || t.getStatus() == STATUS_CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "任务已完成或已取消");
        }
        t.setStatus(STATUS_DONE);
        t.setProgress(100);
        t.setCompletedTime(LocalDateTime.now());
        taskMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long tenantId = requireTenantId();
        OaTask t = loadTask(id, tenantId);
        if (t.getStatus() == STATUS_DONE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已完成的任务不可取消");
        }
        t.setStatus(STATUS_CANCELLED);
        taskMapper.updateById(t);
    }

    // 评论
    public List<TaskCommentVO> listComments(Long taskId) {
        Long tenantId = requireTenantId();
        loadTask(taskId, tenantId);
        return commentMapper.selectList(new LambdaQueryWrapper<OaTaskComment>()
                        .eq(OaTaskComment::getTenantId, tenantId)
                        .eq(OaTaskComment::getTaskId, taskId)
                        .eq(OaTaskComment::getDelFlag, 0)
                        .orderByAsc(OaTaskComment::getCreateTime))
                .stream().map(c -> {
                    TaskCommentVO vo = new TaskCommentVO();
                    vo.setId(c.getId());
                    vo.setUserId(c.getUserId());
                    vo.setUserName(c.getUserName());
                    vo.setContent(c.getContent());
                    vo.setCreateTime(c.getCreateTime() != null
                            ? c.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                    return vo;
                }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void addComment(Long taskId, String content) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        String userName = SecurityUtils.currentPrincipal().getUsername();
        loadTask(taskId, tenantId);

        OaTaskComment c = new OaTaskComment();
        c.setTenantId(tenantId);
        c.setTaskId(taskId);
        c.setUserId(userId);
        c.setUserName(userName);
        c.setContent(content);
        commentMapper.insert(c);
    }

    private OaTask loadTask(Long id, Long tenantId) {
        OaTask t = taskMapper.selectById(id);
        if (t == null || !Objects.equals(t.getTenantId(), tenantId)
                || (t.getDelFlag() != null && t.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        return t;
    }

    private TaskVO toVO(OaTask t) {
        TaskVO vo = new TaskVO();
        vo.setId(t.getId());
        vo.setTaskNo(t.getTaskNo());
        vo.setTitle(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setPriority(t.getPriority());
        vo.setPriorityLabel(t.getPriority() != null ? switch (t.getPriority()) {
            case 1 -> "紧急";
            case 2 -> "高";
            case 3 -> "中";
            case 4 -> "低";
            default -> "未知";
        } : null);
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(t.getStatus() != null ? switch (t.getStatus()) {
            case 0 -> "待接受";
            case 1 -> "进行中";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        } : null);
        vo.setAssigneeUserId(t.getAssigneeUserId());
        vo.setAssigneeUserName(t.getAssigneeUserName());
        vo.setCreatorUserId(t.getCreatorUserId());
        vo.setDueDate(t.getDueDate() != null ? t.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setStartDate(t.getStartDate() != null ? t.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setCompletedTime(t.getCompletedTime() != null
                ? t.getCompletedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        vo.setProgress(t.getProgress());
        vo.setTags(t.getTags());
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

    // ===================== DTO =====================

    @lombok.Data
    public static class TaskCreateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @jakarta.validation.constraints.NotBlank
        private String title;
        private String description;
        private Integer priority;
        private Long assigneeUserId;
        private String assigneeUserName;
        private String dueDate;
        private String startDate;
        private String tags;
    }

    @lombok.Data
    public static class TaskUpdateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String title;
        private String description;
        private Integer priority;
        private Long assigneeUserId;
        private String assigneeUserName;
        private String dueDate;
        private String tags;
    }

    @lombok.Data
    public static class TaskVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String taskNo;
        private String title;
        private String description;
        private Integer priority;
        private String priorityLabel;
        private Integer status;
        private String statusLabel;
        private Long assigneeUserId;
        private String assigneeUserName;
        private Long creatorUserId;
        private String dueDate;
        private String startDate;
        private String completedTime;
        private Integer progress;
        private String tags;
        private String createTime;
    }

    @lombok.Data
    public static class TaskCommentVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long userId;
        private String userName;
        private String content;
        private String createTime;
    }
}
