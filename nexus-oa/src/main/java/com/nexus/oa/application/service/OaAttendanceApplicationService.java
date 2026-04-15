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
import com.nexus.oa.infrastructure.mapper.OaAttendanceRuleMapper;
import com.nexus.oa.infrastructure.mapper.OaAttendanceRecordMapper;
import com.nexus.oa.infrastructure.mapper.OaLeaveDetailMapper;
import com.nexus.oa.infrastructure.mapper.OaOvertimeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaAttendanceApplicationService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 考勤状态
    private static final int STATUS_NORMAL = 0;    // 正常
    private static final int STATUS_LATE = 1;      // 迟到
    private static final int STATUS_EARLY = 2;   // 早退
    private static final int STATUS_MISSING = 3;  // 缺卡
    private static final int STATUS_ABSENT = 4;    // 旷工
    private static final int STATUS_OVERTIME = 5;  // 加班

    // 请假状态
    private static final int LEAVE_DRAFT = 0;
    private static final int LEAVE_PENDING = 1;
    private static final int LEAVE_APPROVED = 2;
    private static final int LEAVE_REJECTED = 3;

    // 加班状态
    private static final int OT_DRAFT = 0;
    private static final int OT_PENDING = 1;
    private static final int OT_APPROVED = 2;
    private static final int OT_REJECTED = 3;

    private final OaAttendanceRuleMapper ruleMapper;
    private final OaAttendanceRecordMapper recordMapper;
    private final OaLeaveDetailMapper leaveMapper;
    private final OaOvertimeMapper overtimeMapper;

    // ===================== 考勤规则 =====================

    public List<OaDtos.AttendanceRuleVO> listRules() {
        Long tenantId = requireTenantId();
        return ruleMapper.selectList(new LambdaQueryWrapper<OaAttendanceRule>()
                        .eq(OaAttendanceRule::getTenantId, tenantId)
                        .eq(OaAttendanceRule::getDelFlag, 0)
                        .eq(OaAttendanceRule::getIsEnable, 1)
                        .orderByAsc(OaAttendanceRule::getId)
                        .last("LIMIT 1"))
                .stream().map(this::toRuleVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRule(OaDtos.AttendanceRuleCreateRequest req) {
        Long tenantId = requireTenantId();
        OaAttendanceRule rule = new OaAttendanceRule();
        rule.setTenantId(tenantId);
        rule.setRuleName(req.getRuleName());
        rule.setCheckInStart(LocalTime.parse(req.getCheckInStart()));
        rule.setCheckInEnd(LocalTime.parse(req.getCheckInEnd()));
        rule.setCheckOutStart(LocalTime.parse(req.getCheckOutStart()));
        rule.setCheckOutEnd(LocalTime.parse(req.getCheckOutEnd()));
        rule.setIsEnable(1);
        rule.setRemark(req.getRemark());
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        Long tenantId = requireTenantId();
        OaAttendanceRule r = ruleMapper.selectById(id);
        if (r == null || !Objects.equals(r.getTenantId(), tenantId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        ruleMapper.deleteById(id);
    }

    // ===================== 打卡 =====================

    @Transactional(rollbackFor = Exception.class)
    public void checkIn(OaDtos.CheckInRequest req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 查询是否有今日记录
        OaAttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<OaAttendanceRecord>()
                .eq(OaAttendanceRecord::getTenantId, tenantId)
                .eq(OaAttendanceRecord::getUserId, userId)
                .eq(OaAttendanceRecord::getCheckDate, today)
                .eq(OaAttendanceRecord::getDelFlag, 0));

        OaAttendanceRule rule = getActiveRule(tenantId);
        LocalTime currentTime = now.toLocalTime();

        if ("in".equals(req.getType())) {
            if (record != null && record.getCheckInTime() != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "今日已打过上班卡");
            }
            OaAttendanceRecord newRecord = new OaAttendanceRecord();
            newRecord.setTenantId(tenantId);
            newRecord.setUserId(userId);
            newRecord.setCheckDate(today);
            newRecord.setCheckInTime(now);
            newRecord.setIsOuter(req.getIsOuter() != null ? req.getIsOuter() : 0);
            if (req.getIsOuter() != null && req.getIsOuter() == 1) {
                newRecord.setOuterAddress(req.getOuterAddress());
                newRecord.setOuterReason(req.getOuterReason());
            }
            // 根据规则判断状态
            if (rule != null && currentTime.isAfter(rule.getCheckInEnd())) {
                newRecord.setStatus(STATUS_LATE); // 迟到
            } else {
                newRecord.setStatus(STATUS_NORMAL);
            }
            recordMapper.insert(newRecord);

        } else { // out - 下班打卡
            if (record == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "今日未打过上班卡，无法下班打卡");
            }
            if (record.getCheckOutTime() != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "今日已打过下班卡");
            }
            record.setCheckOutTime(now);
            // 计算工作时长
            if (record.getCheckInTime() != null) {
                long minutes = ChronoUnit.MINUTES.between(record.getCheckInTime(), now);
                record.setWorkMinutes((int) minutes);
            }
            // 判断是否早退
            if (rule != null && currentTime.isBefore(rule.getCheckOutStart())) {
                record.setStatus(STATUS_EARLY); // 早退
            } else if (record.getStatus() == STATUS_LATE) {
                record.setStatus(STATUS_NORMAL); // 有迟到但正常下班，合并为正常
            }
            recordMapper.updateById(record);
        }
    }

    public OaDtos.TodayStatusVO getTodayStatus() {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        LocalDate today = LocalDate.now();

        OaAttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<OaAttendanceRecord>()
                .eq(OaAttendanceRecord::getTenantId, tenantId)
                .eq(OaAttendanceRecord::getUserId, userId)
                .eq(OaAttendanceRecord::getCheckDate, today)
                .eq(OaAttendanceRecord::getDelFlag, 0));

        OaDtos.TodayStatusVO vo = new OaDtos.TodayStatusVO();
        if (record != null) {
            vo.setCheckedIn(record.getCheckInTime() != null);
            vo.setCheckedOut(record.getCheckOutTime() != null);
            vo.setCheckInTime(record.getCheckInTime() != null
                    ? record.getCheckInTime().format(DT_FMT) : null);
            vo.setCheckOutTime(record.getCheckOutTime() != null
                    ? record.getCheckOutTime().format(DT_FMT) : null);
            vo.setStatus(record.getStatus());
            vo.setStatusLabel(getStatusLabel(record.getStatus()));
        } else {
            vo.setCheckedIn(false);
            vo.setCheckedOut(false);
            vo.setStatus(STATUS_ABSENT);
            vo.setStatusLabel("旷工");
        }
        return vo;
    }

    public IPage<OaDtos.AttendanceRecordVO> pageRecords(long current, long size,
                                                           Long userId, String dateFrom, String dateTo) {
        Long tenantId = requireTenantId();
        Page<OaAttendanceRecord> p = new Page<>(current, size);
        LambdaQueryWrapper<OaAttendanceRecord> w = new LambdaQueryWrapper<OaAttendanceRecord>()
                .eq(OaAttendanceRecord::getTenantId, tenantId)
                .eq(OaAttendanceRecord::getDelFlag, 0)
                .eq(userId != null, OaAttendanceRecord::getUserId, userId)
                .ge(dateFrom != null && !dateFrom.isBlank(),
                        OaAttendanceRecord::getCheckDate, dateFrom)
                .le(dateTo != null && !dateTo.isBlank(),
                        OaAttendanceRecord::getCheckDate, dateTo)
                .orderByDesc(OaAttendanceRecord::getCheckDate);
        return recordMapper.selectPage(p, w).convert(this::toRecordVO);
    }

    // ===================== 请假（新表） =====================

    public IPage<OaDtos.LeaveVO> pageLeaves(long current, long size, Long userId, Integer status) {
        Long tenantId = requireTenantId();
        Page<OaLeave> p = new Page<>(current, size);
        LambdaQueryWrapper<OaLeave> w = new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getTenantId, tenantId)
                .eq(OaLeave::getDelFlag, 0)
                .eq(userId != null, OaLeave::getUserId, userId)
                .eq(status != null, OaLeave::getStatus, status)
                .orderByDesc(OaLeave::getId);
        return leaveMapper.selectPage(p, w).convert(this::toLeaveVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(OaDtos.LeaveCreateRequest req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        String userName = SecurityUtils.currentPrincipal().getUsername();

        OaLeave leave = new OaLeave();
        leave.setTenantId(tenantId);
        leave.setLeaveNo(nextNo("LV"));
        leave.setUserId(userId);
        leave.setUserName(userName);
        leave.setLeaveType(req.getLeaveType());
        leave.setStartDate(LocalDate.parse(req.getStartDate()));
        leave.setEndDate(LocalDate.parse(req.getEndDate()));
        leave.setLeaveDays(req.getLeaveDays());
        leave.setReason(req.getReason());
        leave.setStatus(LEAVE_DRAFT);
        leaveMapper.insert(leave);
        return leave.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitLeave(Long leaveId) {
        Long tenantId = requireTenantId();
        OaLeave leave = loadLeave(leaveId, tenantId);
        if (leave.getStatus() != LEAVE_DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交");
        }
        leave.setStatus(LEAVE_PENDING);
        leaveMapper.updateById(leave);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveLeave(Long leaveId, OaDtos.LeaveApproveRequest req) {
        Long tenantId = requireTenantId();
        Long approverId = requireUserId();
        OaLeave leave = loadLeave(leaveId, tenantId);
        if (leave.getStatus() != LEAVE_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审批状态可审批");
        }
        leave.setApproverUserId(approverId);
        leave.setApproverOpinion(req.getOpinion());
        leave.setApproverTime(LocalDateTime.now());
        leave.setStatus(req.getApproved() ? LEAVE_APPROVED : LEAVE_REJECTED);
        leaveMapper.updateById(leave);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLeave(Long leaveId) {
        Long tenantId = requireTenantId();
        OaLeave leave = loadLeave(leaveId, tenantId);
        if (leave.getStatus() != LEAVE_DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可删除");
        }
        leaveMapper.deleteById(leaveId);
    }

    // ===================== 加班 =====================

    public IPage<OaDtos.OvertimeVO> pageOvertimes(long current, long size, Long userId, Integer status) {
        Long tenantId = requireTenantId();
        Page<OaOvertime> p = new Page<>(current, size);
        LambdaQueryWrapper<OaOvertime> w = new LambdaQueryWrapper<OaOvertime>()
                .eq(OaOvertime::getTenantId, tenantId)
                .eq(OaOvertime::getDelFlag, 0)
                .eq(userId != null, OaOvertime::getUserId, userId)
                .eq(status != null, OaOvertime::getStatus, status)
                .orderByDesc(OaOvertime::getId);
        return overtimeMapper.selectPage(p, w).convert(this::toOvertimeVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createOvertime(OaDtos.OvertimeCreateRequest req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        String userName = SecurityUtils.currentPrincipal().getUsername();

        OaOvertime ot = new OaOvertime();
        ot.setTenantId(tenantId);
        ot.setOvertimeNo(nextNo("OT"));
        ot.setUserId(userId);
        ot.setUserName(userName);
        ot.setStartTime(LocalDateTime.parse(req.getStartTime(), DT_FMT));
        ot.setEndTime(LocalDateTime.parse(req.getEndTime(), DT_FMT));
        ot.setHours(req.getHours());
        ot.setReason(req.getReason());
        ot.setStatus(OT_DRAFT);
        overtimeMapper.insert(ot);
        return ot.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitOvertime(Long overtimeId) {
        Long tenantId = requireTenantId();
        OaOvertime ot = loadOvertime(overtimeId, tenantId);
        if (ot.getStatus() != OT_DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可提交");
        }
        ot.setStatus(OT_PENDING);
        overtimeMapper.updateById(ot);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveOvertime(Long overtimeId, OaDtos.LeaveApproveRequest req) {
        Long tenantId = requireTenantId();
        Long approverId = requireUserId();
        OaOvertime ot = loadOvertime(overtimeId, tenantId);
        if (ot.getStatus() != OT_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审批状态可审批");
        }
        ot.setApproverUserId(approverId);
        ot.setApproverOpinion(req.getOpinion());
        ot.setApproverTime(LocalDateTime.now());
        ot.setStatus(req.getApproved() ? OT_APPROVED : OT_REJECTED);
        overtimeMapper.updateById(ot);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOvertime(Long overtimeId) {
        Long tenantId = requireTenantId();
        OaOvertime ot = loadOvertime(overtimeId, tenantId);
        if (ot.getStatus() != OT_DRAFT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅草稿状态可删除");
        }
        overtimeMapper.deleteById(overtimeId);
    }

    // ===================== 考勤统计 =====================

    public OaDtos.AttendanceStatisticsVO statistics(Long userId, int year, int month) {
        Long tenantId = requireTenantId();
        Long targetUserId = userId != null ? userId : requireUserId();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<OaAttendanceRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<OaAttendanceRecord>()
                        .eq(OaAttendanceRecord::getTenantId, tenantId)
                        .eq(OaAttendanceRecord::getUserId, targetUserId)
                        .eq(OaAttendanceRecord::getDelFlag, 0)
                        .ge(OaAttendanceRecord::getCheckDate, start)
                        .le(OaAttendanceRecord::getCheckDate, end));

        OaDtos.AttendanceStatisticsVO stat = new OaDtos.AttendanceStatisticsVO();
        int presentDays = 0, lateDays = 0, earlyDays = 0, absentDays = 0, missingDays = 0;
        for (OaAttendanceRecord r : records) {
            if (r.getCheckInTime() != null) presentDays++;
            if (r.getStatus() != null) {
                if (r.getStatus() == STATUS_LATE) lateDays++;
                else if (r.getStatus() == STATUS_EARLY) earlyDays++;
                else if (r.getStatus() == STATUS_MISSING) missingDays++;
                else if (r.getStatus() == STATUS_ABSENT) absentDays++;
            }
        }
        stat.setPresentDays(presentDays);
        stat.setLateDays(lateDays);
        stat.setEarlyLeaveDays(earlyDays);
        stat.setAbsentDays(absentDays);
        stat.setMissingCardDays(missingDays);
        stat.setOvertimeHours(0);
        return stat;
    }

    // ===================== 私有方法 =====================

    private OaAttendanceRule getActiveRule(Long tenantId) {
        return ruleMapper.selectOne(new LambdaQueryWrapper<OaAttendanceRule>()
                .eq(OaAttendanceRule::getTenantId, tenantId)
                .eq(OaAttendanceRule::getDelFlag, 0)
                .eq(OaAttendanceRule::getIsEnable, 1)
                .orderByAsc(OaAttendanceRule::getId)
                .last("LIMIT 1"));
    }

    private OaLeave loadLeave(Long id, Long tenantId) {
        OaLeave l = leaveMapper.selectById(id);
        if (l == null || !Objects.equals(l.getTenantId(), tenantId)
                || (l.getDelFlag() != null && l.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假记录不存在");
        }
        return l;
    }

    private OaOvertime loadOvertime(Long id, Long tenantId) {
        OaOvertime o = overtimeMapper.selectById(id);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId)
                || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "加班记录不存在");
        }
        return o;
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_NORMAL -> "正常";
            case STATUS_LATE -> "迟到";
            case STATUS_EARLY -> "早退";
            case STATUS_MISSING -> "缺卡";
            case STATUS_ABSENT -> "旷工";
            case STATUS_OVERTIME -> "加班";
            default -> "未知";
        };
    }

    private String getLeaveStatusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case LEAVE_DRAFT -> "草稿";
            case LEAVE_PENDING -> "待审批";
            case LEAVE_APPROVED -> "已通过";
            case LEAVE_REJECTED -> "已拒绝";
            default -> "未知";
        };
    }

    private OaDtos.AttendanceRuleVO toRuleVO(OaAttendanceRule r) {
        OaDtos.AttendanceRuleVO vo = new OaDtos.AttendanceRuleVO();
        vo.setId(r.getId());
        vo.setRuleName(r.getRuleName());
        vo.setCheckInStart(r.getCheckInStart() != null ? r.getCheckInStart().format(TIME_FMT) : null);
        vo.setCheckInEnd(r.getCheckInEnd() != null ? r.getCheckInEnd().format(TIME_FMT) : null);
        vo.setCheckOutStart(r.getCheckOutStart() != null ? r.getCheckOutStart().format(TIME_FMT) : null);
        vo.setCheckOutEnd(r.getCheckOutEnd() != null ? r.getCheckOutEnd().format(TIME_FMT) : null);
        vo.setIsEnable(r.getIsEnable());
        vo.setRemark(r.getRemark());
        return vo;
    }

    private OaDtos.AttendanceRecordVO toRecordVO(OaAttendanceRecord r) {
        OaDtos.AttendanceRecordVO vo = new OaDtos.AttendanceRecordVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setCheckDate(r.getCheckDate() != null ? r.getCheckDate().format(DATE_FMT) : null);
        vo.setCheckInTime(r.getCheckInTime() != null ? r.getCheckInTime().format(DT_FMT) : null);
        vo.setCheckOutTime(r.getCheckOutTime() != null ? r.getCheckOutTime().format(DT_FMT) : null);
        vo.setWorkMinutes(r.getWorkMinutes());
        vo.setStatus(r.getStatus());
        vo.setIsOuter(r.getIsOuter());
        vo.setOuterAddress(r.getOuterAddress());
        vo.setOuterReason(r.getOuterReason());
        vo.setRemark(r.getRemark());
        return vo;
    }

    private OaDtos.LeaveVO toLeaveVO(OaLeave l) {
        OaDtos.LeaveVO vo = new OaDtos.LeaveVO();
        vo.setId(l.getId());
        vo.setLeaveNo(l.getLeaveNo());
        vo.setUserId(l.getUserId());
        vo.setUserName(l.getUserName());
        vo.setLeaveType(l.getLeaveType());
        vo.setStartDate(l.getStartDate() != null ? l.getStartDate().format(DATE_FMT) : null);
        vo.setEndDate(l.getEndDate() != null ? l.getEndDate().format(DATE_FMT) : null);
        vo.setLeaveDays(l.getLeaveDays());
        vo.setReason(l.getReason());
        vo.setStatus(l.getStatus());
        vo.setStatusLabel(getLeaveStatusLabel(l.getStatus()));
        vo.setApproverUserId(l.getApproverUserId());
        vo.setApproverOpinion(l.getApproverOpinion());
        vo.setApproverTime(l.getApproverTime() != null ? l.getApproverTime().format(DT_FMT) : null);
        vo.setCreateTime(l.getCreateTime() != null ? l.getCreateTime().format(DT_FMT) : null);
        return vo;
    }

    private OaDtos.OvertimeVO toOvertimeVO(OaOvertime o) {
        OaDtos.OvertimeVO vo = new OaDtos.OvertimeVO();
        vo.setId(o.getId());
        vo.setOvertimeNo(o.getOvertimeNo());
        vo.setUserId(o.getUserId());
        vo.setUserName(o.getUserName());
        vo.setStartTime(o.getStartTime() != null ? o.getStartTime().format(DT_FMT) : null);
        vo.setEndTime(o.getEndTime() != null ? o.getEndTime().format(DT_FMT) : null);
        vo.setHours(o.getHours());
        vo.setReason(o.getReason());
        vo.setStatus(o.getStatus());
        String label = switch (o.getStatus()) {
            case OT_DRAFT -> "草稿";
            case OT_PENDING -> "待审批";
            case OT_APPROVED -> "已通过";
            case OT_REJECTED -> "已拒绝";
            default -> "未知";
        };
        vo.setStatusLabel(label);
        vo.setApproverUserId(o.getApproverUserId());
        vo.setApproverOpinion(o.getApproverOpinion());
        vo.setApproverTime(o.getApproverTime() != null ? o.getApproverTime().format(DT_FMT) : null);
        vo.setCreateTime(o.getCreateTime() != null ? o.getCreateTime().format(DT_FMT) : null);
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
}
