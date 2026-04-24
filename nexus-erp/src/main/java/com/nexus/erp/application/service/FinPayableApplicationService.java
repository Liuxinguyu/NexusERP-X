package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.FinDtos;
import com.nexus.erp.domain.model.*;
import com.nexus.erp.infrastructure.mapper.*;
import com.nexus.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinPayableApplicationService {

    private static final String SOURCE_PURCHASE_ORDER = "purchase_order";
    private static final int STATUS_UNPAID = 0;      // 未付款
    private static final int STATUS_PARTIAL = 1;     // 部分付款
    private static final int STATUS_SETTLED = 2;     // 已结清

    private final FinPayableMapper payableMapper;
    private final FinPayableRecordMapper recordMapper;
    private final ErpPurchaseOrderMapper purchaseOrderMapper;
    private final ErpSupplierMapper supplierMapper;

    // ===================== 分页查询 =====================

    public IPage<FinDtos.PayableVO> page(long current, long size, Long supplierId, Integer status, String dateFrom, String dateTo) {
        Long tenantId = requireTenantId();
        Page<FinPayable> p = new Page<>(current, size);
        LambdaQueryWrapper<FinPayable> w = new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getTenantId, tenantId)
                .eq(FinPayable::getDelFlag, 0)
                .eq(status != null, FinPayable::getStatus, status)
                .eq(supplierId != null, FinPayable::getSupplierId, supplierId)
                .ge(dateFrom != null && !dateFrom.isBlank(),
                        FinPayable::getCreateTime, dateFrom + " 00:00:00")
                .le(dateTo != null && !dateTo.isBlank(),
                        FinPayable::getCreateTime, dateTo + " 23:59:59")
                .orderByDesc(FinPayable::getId);
        IPage<FinPayable> result = payableMapper.selectPage(p, w);
        return result.convert(this::toVO);
    }

    public FinDtos.PayableVO getById(Long id) {
        Long tenantId = requireTenantId();
        FinPayable p = loadPayable(id, tenantId);
        return toVO(p);
    }

    public List<FinDtos.PayableRecordVO> listRecords(Long payableId) {
        Long tenantId = requireTenantId();
        loadPayable(payableId, tenantId);
        return recordMapper.selectList(new LambdaQueryWrapper<FinPayableRecord>()
                .eq(FinPayableRecord::getTenantId, tenantId)
                .eq(FinPayableRecord::getPayableId, payableId)
                .eq(FinPayableRecord::getDelFlag, 0)
                .orderByDesc(FinPayableRecord::getId))
                .stream().map(this::toRecordVO).toList();
    }

    // ===================== 创建应付 =====================

    @Transactional(rollbackFor = Exception.class)
    public Long create(FinDtos.PayableCreateRequest req) {
        Long tenantId = requireTenantId();
        validateAmount(req.getTotalAmount(), "应付总金额");
        validateSourceRef(tenantId, req.getSourceType(), req.getSourceId());
        ErpSupplier supplier = loadSupplier(req.getSupplierId(), tenantId);
        FinPayable p = new FinPayable();
        p.setTenantId(tenantId);
        p.setPayableNo(nextNo("AP"));
        p.setSourceType(normalizeSourceType(req.getSourceType()));
        p.setSourceId(req.getSourceId());
        p.setSupplierId(supplier.getId());
        p.setSupplierName(supplier.getSupplierName());
        p.setTotalAmount(req.getTotalAmount());
        p.setPaidAmount(BigDecimal.ZERO);
        p.setPendingAmount(req.getTotalAmount());
        p.setInvoiceNo(req.getInvoiceNo());
        p.setDueDate(req.getDueDate());
        p.setStatus(STATUS_UNPAID);
        p.setRemark(req.getRemark());
        payableMapper.insert(p);
        return p.getId();
    }

    /**
     * 采购订单确认入库时，自动创建应付记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromPurchaseOrder(Long purchaseOrderId) {
        Long tenantId = requireTenantId();
        FinPayable existing = findBySource(tenantId, SOURCE_PURCHASE_ORDER, purchaseOrderId);
        if (existing != null) {
            return existing.getId();
        }

        ErpPurchaseOrder order = loadPurchaseOrder(purchaseOrderId, tenantId);
        ErpSupplier supplier = loadSupplier(order.getSupplierId(), tenantId);

        FinPayable p = new FinPayable();
        p.setTenantId(tenantId);
        p.setPayableNo(nextNo("AP"));
        p.setSourceType(SOURCE_PURCHASE_ORDER);
        p.setSourceId(order.getId());
        p.setSupplierId(order.getSupplierId());
        p.setSupplierName(supplier.getSupplierName());
        p.setTotalAmount(order.getTotalAmount());
        p.setPaidAmount(BigDecimal.ZERO);
        p.setPendingAmount(order.getTotalAmount());
        p.setStatus(STATUS_UNPAID);
        try {
            payableMapper.insert(p);
            return p.getId();
        } catch (DuplicateKeyException ex) {
            FinPayable duplicate = findBySource(tenantId, SOURCE_PURCHASE_ORDER, purchaseOrderId);
            if (duplicate != null) {
                return duplicate.getId();
            }
            throw ex;
        }
    }

    // ===================== 更新应付 =====================

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinDtos.PayableUpdateRequest req) {
        Long tenantId = requireTenantId();
        FinPayable p = loadPayable(id, tenantId);
        if (Objects.equals(p.getStatus(), STATUS_SETTLED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已结清的单据不可修改");
        }
        p.setInvoiceNo(req.getInvoiceNo());
        p.setDueDate(req.getDueDate());
        p.setRemark(req.getRemark());
        payableMapper.updateById(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        FinPayable p = loadPayable(id, tenantId);
        if (!Objects.equals(p.getStatus(), STATUS_UNPAID)
                || (p.getPaidAmount() != null && p.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已有付款记录的单据不可删除");
        }
        payableMapper.deleteById(id);
    }

    // ===================== 登记付款 =====================

    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(Long payableId, FinDtos.PayableRecordCreate req) {
        Long tenantId = requireTenantId();
        Long userId = SecurityUtils.currentUserId() != null ? SecurityUtils.currentUserId() : 0L;
        FinPayable p = loadPayable(payableId, tenantId);

        if (Objects.equals(p.getStatus(), STATUS_SETTLED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该单据已结清，无需再付款");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "付款金额必须大于 0");
        }
        if (req.getAmount().compareTo(p.getPendingAmount()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "付款金额不可超过待付金额，当前待付：" + p.getPendingAmount());
        }

        // 金额与状态机：paid += amount, pending -= amount
        BigDecimal newPaid = p.getPaidAmount().add(req.getAmount());
        BigDecimal newPending = p.getPendingAmount().subtract(req.getAmount());
        p.setPaidAmount(newPaid);
        if (newPending.compareTo(BigDecimal.ZERO) <= 0) {
            p.setPendingAmount(BigDecimal.ZERO);
            p.setStatus(STATUS_SETTLED);
        } else {
            p.setPendingAmount(newPending);
            p.setStatus(STATUS_PARTIAL);
        }

        // 并发阻断：依赖 @Version 乐观锁（updateById 会带 WHERE id=? AND version=?）
        int rows = payableMapper.updateById(p);
        if (rows == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "账单状态已发生变化，请刷新页面后重试");
        }

        // 仅在应付主单更新成功后，插入付款记录（同事务内，失败会回滚主单更新）
        FinPayableRecord record = new FinPayableRecord();
        record.setTenantId(tenantId);
        record.setPayableId(payableId);
        record.setRecordNo(nextNo("PR"));
        record.setAmount(req.getAmount());
        record.setPaymentMethod(req.getPaymentMethod());
        record.setPaymentAccount(req.getPaymentAccount());
        record.setPaymentTime(req.getPaymentTime() != null ? req.getPaymentTime() : LocalDateTime.now());
        record.setHandlerUserId(userId);
        record.setReceiptUrl(req.getReceiptUrl());
        record.setRemark(req.getRemark());
        recordMapper.insert(record);
    }

    // ===================== 汇总 =====================

    public FinDtos.PayableSummary summary(Long supplierId, String month) {
        Long tenantId = requireTenantId();
        FinDtos.PayableSummary s = new FinDtos.PayableSummary();

        LambdaQueryWrapper<FinPayable> w = new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getTenantId, tenantId)
                .eq(FinPayable::getDelFlag, 0)
                .eq(supplierId != null, FinPayable::getSupplierId, supplierId);
        MonthRange range = parseMonthRangeOrThrow(month, "month");
        if (range != null) {
            w.ge(FinPayable::getCreateTime, range.start)
             .lt(FinPayable::getCreateTime, range.endExclusive);
        }

        List<FinPayable> list = payableMapper.selectList(w);

        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long unpaid = 0, partPaid = 0, settled = 0;

        for (FinPayable p : list) {
            totalPayable = totalPayable.add(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO);
            totalPaid = totalPaid.add(p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO);
            totalPending = totalPending.add(p.getPendingAmount() != null ? p.getPendingAmount() : BigDecimal.ZERO);
            if (Objects.equals(p.getStatus(), STATUS_UNPAID)) unpaid++;
            else if (Objects.equals(p.getStatus(), STATUS_PARTIAL)) partPaid++;
            else settled++;
        }
        s.setTotalPayable(totalPayable);
        s.setTotalPaid(totalPaid);
        s.setTotalPending(totalPending);
        s.setUnpaidCount(unpaid);
        s.setPartPaidCount(partPaid);
        s.setSettledCount(settled);
        return s;
    }

    private static MonthRange parseMonthRangeOrThrow(String month, String fieldLabel) {
        if (month == null || month.isBlank()) {
            return null;
        }
        try {
            YearMonth ym = YearMonth.parse(month.trim(), DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();
            return new MonthRange(start, endExclusive);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "格式非法，必须为yyyy-MM");
        }
    }

    private record MonthRange(LocalDateTime start, LocalDateTime endExclusive) {
    }

    // ===================== 私有方法 =====================

    private FinPayable findBySource(Long tenantId, String sourceType, Long sourceId) {
        return payableMapper.selectOne(new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getTenantId, tenantId)
                .eq(FinPayable::getDelFlag, 0)
                .eq(FinPayable::getSourceType, sourceType)
                .eq(FinPayable::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    private FinPayable loadPayable(Long id, Long tenantId) {
        FinPayable p = payableMapper.selectById(id);
        if (p == null || !Objects.equals(p.getTenantId(), tenantId)
                || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "应付单不存在");
        }
        return p;
    }

    private ErpPurchaseOrder loadPurchaseOrder(Long id, Long tenantId) {
        ErpPurchaseOrder o = purchaseOrderMapper.selectById(id);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId)
                || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "采购单不存在");
        }
        return o;
    }

    private ErpSupplier loadSupplier(Long id, Long tenantId) {
        ErpSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !Objects.equals(supplier.getTenantId(), tenantId)
                || (supplier.getDelFlag() != null && supplier.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "供应商不存在或无权访问");
        }
        return supplier;
    }

    private String normalizeSourceType(String sourceType) {
        return StringUtils.hasText(sourceType) ? sourceType.trim().toLowerCase() : SOURCE_PURCHASE_ORDER;
    }

    private void validateSourceRef(Long tenantId, String sourceType, Long sourceId) {
        String normalized = normalizeSourceType(sourceType);
        if (!SOURCE_PURCHASE_ORDER.equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "sourceType非法，仅支持 purchase_order");
        }
        if (sourceId == null || sourceId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "sourceId不能为空且必须大于0");
        }
        loadPurchaseOrder(sourceId, tenantId);
    }

    private void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + "必须大于等于0");
        }
    }

    private FinDtos.PayableVO toVO(FinPayable p) {
        FinDtos.PayableVO vo = new FinDtos.PayableVO();
        vo.setId(p.getId());
        vo.setPayableNo(p.getPayableNo());
        vo.setSourceType(p.getSourceType());
        vo.setSourceId(p.getSourceId());
        vo.setSupplierId(p.getSupplierId());
        vo.setSupplierName(p.getSupplierName());
        vo.setTotalAmount(p.getTotalAmount());
        vo.setPaidAmount(p.getPaidAmount());
        vo.setPendingAmount(p.getPendingAmount());
        vo.setInvoiceNo(p.getInvoiceNo());
        vo.setDueDate(p.getDueDate());
        vo.setStatus(p.getStatus());
        vo.setRemark(p.getRemark());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private FinDtos.PayableRecordVO toRecordVO(FinPayableRecord r) {
        FinDtos.PayableRecordVO vo = new FinDtos.PayableRecordVO();
        vo.setId(r.getId());
        vo.setRecordNo(r.getRecordNo());
        vo.setAmount(r.getAmount());
        vo.setPaymentMethod(r.getPaymentMethod());
        vo.setPaymentAccount(r.getPaymentAccount());
        vo.setPaymentTime(r.getPaymentTime());
        vo.setHandlerUserId(r.getHandlerUserId());
        vo.setReceiptUrl(r.getReceiptUrl());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private static String nextNo(String prefix) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(28);
        return prefix + ts + uuidPart;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }

}
