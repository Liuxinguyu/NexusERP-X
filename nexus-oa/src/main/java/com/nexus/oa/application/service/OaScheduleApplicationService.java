package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.SecurityUtils;
import com.nexus.oa.domain.model.OaSchedule;
import com.nexus.oa.infrastructure.mapper.OaScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OaScheduleApplicationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OaScheduleMapper scheduleMapper;

    public List<ScheduleVO> list(String startDate, String endDate) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        LambdaQueryWrapper<OaSchedule> w = new LambdaQueryWrapper<OaSchedule>()
                .eq(OaSchedule::getTenantId, tenantId)
                .eq(OaSchedule::getDelFlag, 0)
                .and(wr -> wr
                        .eq(OaSchedule::getCreatorUserId, userId)
                        .or()
                        .eq(OaSchedule::getVisibility, 1))
                .le(startDate != null && !startDate.isBlank(),
                        OaSchedule::getStartTime, endDate + " 23:59:59")
                .ge(endDate != null && !endDate.isBlank(),
                        OaSchedule::getEndTime, startDate + " 00:00:00")
                .orderByAsc(OaSchedule::getStartTime);
        return scheduleMapper.selectList(w).stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ScheduleCreateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();

        OaSchedule s = new OaSchedule();
        s.setTenantId(tenantId);
        s.setTitle(req.getTitle().trim());
        s.setContent(req.getContent());
        s.setStartTime(LocalDateTime.parse(req.getStartTime(), DT_FMT));
        s.setEndTime(LocalDateTime.parse(req.getEndTime(), DT_FMT));
        s.setIsAllDay(req.getIsAllDay() != null ? req.getIsAllDay() : 0);
        s.setReminderMinutes(req.getReminderMinutes());
        s.setLocation(req.getLocation());
        s.setColor(req.getColor() != null ? req.getColor() : "#409EFF");
        s.setVisibility(req.getVisibility() != null ? req.getVisibility() : 0);
        s.setCreatorUserId(userId);
        scheduleMapper.insert(s);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ScheduleUpdateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaSchedule s = loadSchedule(id, tenantId);
        if (!Objects.equals(s.getCreatorUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有创建人可以修改日程");
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) s.setTitle(req.getTitle().trim());
        if (req.getContent() != null) s.setContent(req.getContent());
        if (req.getStartTime() != null && !req.getStartTime().isBlank())
            s.setStartTime(LocalDateTime.parse(req.getStartTime(), DT_FMT));
        if (req.getEndTime() != null && !req.getEndTime().isBlank())
            s.setEndTime(LocalDateTime.parse(req.getEndTime(), DT_FMT));
        if (req.getIsAllDay() != null) s.setIsAllDay(req.getIsAllDay());
        if (req.getReminderMinutes() != null) s.setReminderMinutes(req.getReminderMinutes());
        if (req.getLocation() != null) s.setLocation(req.getLocation());
        if (req.getColor() != null) s.setColor(req.getColor());
        if (req.getVisibility() != null) s.setVisibility(req.getVisibility());
        scheduleMapper.updateById(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        OaSchedule s = loadSchedule(id, tenantId);
        if (!Objects.equals(s.getCreatorUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有创建人可以删除日程");
        }
        scheduleMapper.deleteById(id);
    }

    private OaSchedule loadSchedule(Long id, Long tenantId) {
        OaSchedule s = scheduleMapper.selectById(id);
        if (s == null || !Objects.equals(s.getTenantId(), tenantId)
                || (s.getDelFlag() != null && s.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "日程不存在");
        }
        return s;
    }

    private ScheduleVO toVO(OaSchedule s) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setContent(s.getContent());
        vo.setStartTime(s.getStartTime() != null ? s.getStartTime().format(DT_FMT) : null);
        vo.setEndTime(s.getEndTime() != null ? s.getEndTime().format(DT_FMT) : null);
        vo.setIsAllDay(s.getIsAllDay());
        vo.setReminderMinutes(s.getReminderMinutes());
        vo.setLocation(s.getLocation());
        vo.setColor(s.getColor());
        vo.setVisibility(s.getVisibility());
        return vo;
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
    public static class ScheduleCreateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @jakarta.validation.constraints.NotBlank
        private String title;
        private String content;
        @jakarta.validation.constraints.NotBlank
        private String startTime;
        @jakarta.validation.constraints.NotBlank
        private String endTime;
        private Integer isAllDay;
        private Integer reminderMinutes;
        private String location;
        private String color;
        private Integer visibility;
    }

    @lombok.Data
    public static class ScheduleUpdateReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String title;
        private String content;
        private String startTime;
        private String endTime;
        private Integer isAllDay;
        private Integer reminderMinutes;
        private String location;
        private String color;
        private Integer visibility;
    }

    @lombok.Data
    public static class ScheduleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String title;
        private String content;
        private String startTime;
        private String endTime;
        private Integer isAllDay;
        private Integer reminderMinutes;
        private String location;
        private String color;
        private Integer visibility;
    }
}
