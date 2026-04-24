package com.nexus.wage.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.oa.domain.model.OaEmployee;
import com.nexus.oa.infrastructure.mapper.OaEmployeeMapper;
import com.nexus.wage.application.dto.WageDtos;
import com.nexus.wage.domain.model.WageItemConfig;
import com.nexus.wage.domain.model.WageMonthlySlip;
import com.nexus.wage.infrastructure.mapper.WageItemConfigMapper;
import com.nexus.wage.infrastructure.mapper.WageMonthlySlipMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WageMonthlySlipApplicationService {

    private static final int KIND_BASE = 1;
    private static final int KIND_SUBSIDY = 2;
    private static final int KIND_DEDUCTION = 3;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PAID = 1;

    private final WageMonthlySlipMapper wageMonthlySlipMapper;
    private final WageItemConfigMapper wageItemConfigMapper;
    private final OaEmployeeMapper oaEmployeeMapper;

    public WageMonthlySlipApplicationService(
            WageMonthlySlipMapper wageMonthlySlipMapper,
            WageItemConfigMapper wageItemConfigMapper,
            OaEmployeeMapper oaEmployeeMapper) {
        this.wageMonthlySlipMapper = wageMonthlySlipMapper;
        this.wageItemConfigMapper = wageItemConfigMapper;
        this.oaEmployeeMapper = oaEmployeeMapper;
    }

    public IPage<WageMonthlySlip> list(long current, long size, String belongMonth) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<WageMonthlySlip> w = new LambdaQueryWrapper<WageMonthlySlip>()
                .eq(WageMonthlySlip::getTenantId, tenantId)
                .eq(WageMonthlySlip::getDelFlag, 0)
                .orderByDesc(WageMonthlySlip::getBelongMonth)
                .orderByDesc(WageMonthlySlip::getId);
        if (belongMonth != null && !belongMonth.isBlank()) {
            if (!belongMonth.trim().matches("\\d{4}-(0[1-9]|1[0-2])")) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "月份格式非法，应为 YYYY-MM");
            }
            w.eq(WageMonthlySlip::getBelongMonth, belongMonth.trim());
        }
        return wageMonthlySlipMapper.selectPage(new Page<>(current, size), w);
    }

    public WageMonthlySlip getById(Long id) {
        Long tenantId = requireTenantId();
        WageMonthlySlip s = wageMonthlySlipMapper.selectById(id);
        if (s == null || !Objects.equals(s.getTenantId(), tenantId) || (s.getDelFlag() != null && s.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工资单不存在");
        }
        return s;
    }

    @Transactional(rollbackFor = Exception.class)
    public int generateMonthly(WageDtos.GenerateMonthlyRequest req) {
        Long tenantId = requireTenantId();
        String month = req.getBelongMonth().trim();

        List<WageItemConfig> configs = wageItemConfigMapper.selectList(new LambdaQueryWrapper<WageItemConfig>()
                .eq(WageItemConfig::getTenantId, tenantId)
                .eq(WageItemConfig::getDelFlag, 0));

        BigDecimal base = BigDecimal.ZERO;
        BigDecimal subsidy = BigDecimal.ZERO;
        BigDecimal deduction = BigDecimal.ZERO;
        for (WageItemConfig c : configs) {
            BigDecimal amt = c.getDefaultAmount() != null ? c.getDefaultAmount() : BigDecimal.ZERO;
            int kind = c.getItemKind() != null ? c.getItemKind() : 0;
            switch (kind) {
                case KIND_BASE -> base = base.add(amt);
                case KIND_SUBSIDY -> subsidy = subsidy.add(amt);
                case KIND_DEDUCTION -> deduction = deduction.add(amt);
                default -> throw new BusinessException(ResultCode.INTERNAL_ERROR,
                        "薪资项 [" + c.getItemName() + "] 的类型值 " + kind + " 不合法");
            }
        }

        List<Long> employeeIds = resolveEmployeeIds(tenantId, req.getEmployeeIds());
        if (employeeIds.isEmpty()) {
            return 0;
        }

        Set<Long> existingEmpIds = wageMonthlySlipMapper.selectList(new LambdaQueryWrapper<WageMonthlySlip>()
                        .eq(WageMonthlySlip::getTenantId, tenantId)
                        .eq(WageMonthlySlip::getBelongMonth, month)
                        .eq(WageMonthlySlip::getDelFlag, 0)
                        .in(WageMonthlySlip::getEmployeeId, employeeIds)
                        .select(WageMonthlySlip::getEmployeeId))
                .stream()
                .map(WageMonthlySlip::getEmployeeId)
                .collect(Collectors.toSet());

        int inserted = 0;
        for (Long empId : employeeIds) {
            if (existingEmpIds.contains(empId)) {
                continue;
            }
            WageMonthlySlip slip = new WageMonthlySlip();
            slip.setTenantId(tenantId);
            slip.setBelongMonth(month);
            slip.setEmployeeId(empId);
            slip.setBaseSalary(base);
            slip.setSubsidyTotal(subsidy);
            slip.setDeductionTotal(deduction);
            slip.setNetPay(calcNet(base, subsidy, deduction));
            slip.setStatus(STATUS_PENDING);
            try {
                wageMonthlySlipMapper.insert(slip);
                inserted++;
            } catch (DuplicateKeyException e) {
                log.warn("工资单已存在: month={}, empId={}", month, empId);
            }
        }
        return inserted;
    }

    private List<Long> resolveEmployeeIds(Long tenantId, List<Long> fromRequest) {
        if (!CollectionUtils.isEmpty(fromRequest)) {
            List<Long> distinctIds = fromRequest.stream().filter(Objects::nonNull).distinct().toList();
            if (distinctIds.isEmpty()) {
                return List.of();
            }
            long validCount = oaEmployeeMapper.selectCount(new LambdaQueryWrapper<OaEmployee>()
                    .eq(OaEmployee::getTenantId, tenantId)
                    .eq(OaEmployee::getDelFlag, 0)
                    .in(OaEmployee::getId, distinctIds));
            if (validCount != distinctIds.size()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "包含不属于当前租户的员工");
            }
            return distinctIds;
        }
        List<OaEmployee> employees = oaEmployeeMapper.selectList(new LambdaQueryWrapper<OaEmployee>()
                .eq(OaEmployee::getTenantId, tenantId)
                .eq(OaEmployee::getDelFlag, 0)
                .eq(OaEmployee::getStatus, 1));
        return employees.stream().map(OaEmployee::getId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long id, WageDtos.AdjustSlipRequest req) {
        WageMonthlySlip slip = getById(id);
        if (Objects.equals(slip.getStatus(), STATUS_PAID)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已发放的工资单不可调整");
        }
        slip.setBaseSalary(req.getBaseSalary());
        slip.setSubsidyTotal(req.getSubsidyTotal());
        slip.setDeductionTotal(req.getDeductionTotal());
        slip.setNetPay(calcNet(req.getBaseSalary(), req.getSubsidyTotal(), req.getDeductionTotal()));
        int rows = wageMonthlySlipMapper.updateById(slip);
        if (rows == 0) {
            throw new OptimisticLockingFailureException("工资单已被其他操作修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int confirmPay(WageDtos.ConfirmPayRequest req) {
        Long tenantId = requireTenantId();
        Set<Long> ids = req.getSlipIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return 0;
        }

        List<WageMonthlySlip> slips = wageMonthlySlipMapper.selectList(new LambdaQueryWrapper<WageMonthlySlip>()
                .in(WageMonthlySlip::getId, ids)
                .eq(WageMonthlySlip::getTenantId, tenantId)
                .eq(WageMonthlySlip::getDelFlag, 0));

        if (slips.size() != ids.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部分工资单不存在或不属于当前租户");
        }

        int updated = 0;
        for (WageMonthlySlip slip : slips) {
            if (Objects.equals(slip.getStatus(), STATUS_PAID)) {
                continue;
            }
            slip.setStatus(STATUS_PAID);
            int rows = wageMonthlySlipMapper.updateById(slip);
            if (rows == 0) {
                throw new OptimisticLockingFailureException("工资单 " + slip.getId() + " 已被其他操作修改，请刷新后重试");
            }
            updated++;
        }
        return updated;
    }

    private static BigDecimal calcNet(BigDecimal base, BigDecimal subsidy, BigDecimal deduction) {
        BigDecimal b = base != null ? base : BigDecimal.ZERO;
        BigDecimal s = subsidy != null ? subsidy : BigDecimal.ZERO;
        BigDecimal d = deduction != null ? deduction : BigDecimal.ZERO;
        return b.add(s).subtract(d);
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
