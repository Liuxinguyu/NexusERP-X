package com.nexus.system.application.service;

import com.nexus.common.context.TenantContext;
import com.nexus.common.security.SecurityUtils;
import com.nexus.system.application.dto.WorkbenchDtos;
import com.nexus.system.infrastructure.mapper.WorkbenchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private final WorkbenchMapper workbenchMapper;

    public WorkbenchDtos.DashboardSummary dashboardSummary() {
        Long tenantId = requireTenantId();
        Long userId = SecurityUtils.currentUserId();

        WorkbenchDtos.DashboardSummary summary = new WorkbenchDtos.DashboardSummary();
        summary.setTodaySaleAmount(
                workbenchMapper.todaySaleAmount(tenantId) != null
                        ? workbenchMapper.todaySaleAmount(tenantId)
                        : BigDecimal.ZERO);
        summary.setMonthlyPurchaseAmount(
                workbenchMapper.monthlyPurchaseAmount(tenantId) != null
                        ? workbenchMapper.monthlyPurchaseAmount(tenantId)
                        : BigDecimal.ZERO);
        summary.setCustomerCount(workbenchMapper.customerCount(tenantId));
        summary.setSupplierCount(workbenchMapper.supplierCount(tenantId));
        summary.setPendingApprovalCount(workbenchMapper.pendingApprovalCount(tenantId, userId));
        summary.setStockAlarmCount(workbenchMapper.stockAlarmCount(tenantId));
        return summary;
    }

    public WorkbenchDtos.ChartData saleChart() {
        Long tenantId = requireTenantId();
        WorkbenchDtos.ChartData data = new WorkbenchDtos.ChartData();
        data.setData(workbenchMapper.saleTrend(tenantId));
        return data;
    }

    public WorkbenchDtos.ChartData purchaseChart() {
        Long tenantId = requireTenantId();
        WorkbenchDtos.ChartData data = new WorkbenchDtos.ChartData();
        data.setData(workbenchMapper.purchaseTrend(tenantId));
        return data;
    }

    public List<WorkbenchDtos.TopProduct> topProducts(int limit) {
        Long tenantId = requireTenantId();
        return workbenchMapper.topProducts(tenantId, limit);
    }

    public List<WorkbenchDtos.StockAlarmItem> stockAlarmList(int limit) {
        Long tenantId = requireTenantId();
        return workbenchMapper.stockAlarmList(tenantId, limit);
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new IllegalStateException("缺少租户上下文");
        }
        return tid;
    }
}
