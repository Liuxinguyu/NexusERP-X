package com.nexus.system.infrastructure.mapper;

import com.nexus.system.application.dto.WorkbenchDtos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WorkbenchMapper {

    BigDecimal todaySaleAmount(@Param("tenantId") Long tenantId);

    BigDecimal monthlyPurchaseAmount(@Param("tenantId") Long tenantId);

    Long customerCount(@Param("tenantId") Long tenantId);

    Long supplierCount(@Param("tenantId") Long tenantId);

    Long pendingApprovalCount(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    Long stockAlarmCount(@Param("tenantId") Long tenantId);

    List<WorkbenchDtos.ChartDataPoint> saleTrend(@Param("tenantId") Long tenantId);

    List<WorkbenchDtos.ChartDataPoint> purchaseTrend(@Param("tenantId") Long tenantId);

    List<WorkbenchDtos.TopProduct> topProducts(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    List<WorkbenchDtos.StockAlarmItem> stockAlarmList(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
