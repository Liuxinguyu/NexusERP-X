package com.nexus.wage.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<WageMonthlySlip> list(String belongMonth) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<WageMonthlySlip> w = new LambdaQueryWrapper<WageMonthlySlip>()
                .eq(WageMonthlySlip::getTenantId, tenantId)
                .eq(WageMonthlySlip::getDelFlag, 0)
                .orderByDesc(WageMonthlySlip::getBelongMonth)
                .orderByDesc(WageMonthlySlip::getId);
        if (belongMonth != null && !belongMonth.isBlank()) {
            w.eq(WageMonthlySlip::getBelongMonth, belongMonth.trim());
        }
        return wageMonthlySlipMapper.selectList(w);
    }

    public WageMonthlySlip getById(Long id) {
        Long tenantId = requireTenantId();
        WageMonthlySlip s = wageMonthlySlipMapper.selectById(id);
        if (s == null || !Objects.equals(s.getTenantId(), tenantId) || (s.getDelFlag() != null && s.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工资单不存在");
        }
        return s;
    }

    /**
     * 按年月为员工初始化工资单；金额来自薪资项配置的默认金额，按 item_kind 汇总为基本/补贴/扣款。
     */
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
            int kind = c.getItemKind() != null ? c.getItemKind() : KIND_SUBSIDY;
            switch (kind) {
                case KIND_BASE -> base = base.add(amt);
                case KIND_SUBSIDY -> subsidy = subsidy.add(amt);
                case KIND_DEDUCTION -> deduction = deduction.add(amt);
                default -> subsidy = subsidy.add(amt);
            }
        }

        List<Long> employeeIds = resolveEmployeeIds(tenantId, req.getEmployeeIds());
        int inserted = 0;
        for (Long empId : employeeIds) {
            long exists = wageMonthlySlipMapper.selectCount(new LambdaQueryWrapper<WageMonthlySlip>()
                    .eq(WageMonthlySlip::getTenantId, tenantId)
                    .eq(WageMonthlySlip::getBelongMonth, month)
                    .eq(WageMonthlySlip::getEmployeeId, empId)
                    .eq(WageMonthlySlip::getDelFlag, 0));
            if (exists > 0) {
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
            wageMonthlySlipMapper.insert(slip);
            inserted++;
        }
        return inserted;
    }

    private List<Long> resolveEmployeeIds(Long tenantId, List<Long> fromRequest) {
        if (!CollectionUtils.isEmpty(fromRequest)) {
            return fromRequest.stream().filter(Objects::nonNull).distinct().toList();
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
        wageMonthlySlipMapper.updateById(slip);
    }

    @Transactional(rollbackFor = Exception.class)
    public int confirmPay(WageDtos.ConfirmPayRequest req) {
        Long tenantId = requireTenantId();
        Set<Long> ids = req.getSlipIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return 0;
        }
        int updated = 0;
        for (Long id : ids) {
            WageMonthlySlip slip = wageMonthlySlipMapper.selectById(id);
            if (slip == null || !Objects.equals(slip.getTenantId(), tenantId) || (slip.getDelFlag() != null && slip.getDelFlag() == 1)) {
                throw new BusinessException(ResultCode.NOT_FOUND, "工资单不存在: " + id);
            }
            slip.setStatus(STATUS_PAID);
            wageMonthlySlipMapper.updateById(slip);
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
