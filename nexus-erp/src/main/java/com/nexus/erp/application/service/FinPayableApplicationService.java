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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public IPage<FinDtos.PayableVO> page(long current, long size, FinDtos.PayablePageQuery query) {
        Long tenantId = requireTenantId();
        Page<FinPayable> p = new Page<>(current, size);
        LambdaQueryWrapper<FinPayable> w = new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getTenantId, tenantId)
                .eq(FinPayable::getDelFlag, 0)
                .eq(query.getStatus() != null, FinPayable::getStatus, query.getStatus())
                .eq(query.getSupplierId() != null, FinPayable::getSupplierId, query.getSupplierId())
                .ge(query.getDateFrom() != null && !query.getDateFrom().isBlank(),
                        FinPayable::getCreateTime, query.getDateFrom() + " 00:00:00")
                .le(query.getDateTo() != null && !query.getDateTo().isBlank(),
                        FinPayable::getCreateTime, query.getDateTo() + " 23:59:59")
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
        FinPayable p = new FinPayable();
        p.setTenantId(tenantId);
        p.setPayableNo(nextNo("AP"));
        p.setSourceType(StringUtils.hasText(req.getSourceType()) ? req.getSourceType() : SOURCE_PURCHASE_ORDER);
        p.setSourceId(req.getSourceId() != null ? req.getSourceId() : 0L);
        p.setSupplierId(req.getSupplierId());
        p.setSupplierName(req.getSupplierName());
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
        ErpPurchaseOrder order = loadPurchaseOrder(purchaseOrderId, tenantId);

        ErpSupplier supplier = supplierMapper.selectById(order.getSupplierId());
        String supplierName = supplier != null ? supplier.getSupplierName() : "";

        FinPayable p = new FinPayable();
        p.setTenantId(tenantId);
        p.setPayableNo(nextNo("AP"));
        p.setSourceType(SOURCE_PURCHASE_ORDER);
        p.setSourceId(order.getId());
        p.setSupplierId(order.getSupplierId());
        p.setSupplierName(supplierName);
        p.setTotalAmount(order.getTotalAmount());
        p.setPaidAmount(BigDecimal.ZERO);
        p.setPendingAmount(order.getTotalAmount());
        p.setStatus(STATUS_UNPAID);
        payableMapper.insert(p);
        return p.getId();
    }

    // ===================== 更新应付 =====================

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinDtos.PayableUpdateRequest req) {
        Long tenantId = requireTenantId();
        FinPayable p = loadPayable(id, tenantId);
        if (p.getStatus() == STATUS_SETTLED) {
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
        if (p.getStatus() != STATUS_UNPAID || p.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
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

        if (p.getStatus() == STATUS_SETTLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该单据已结清，无需再付款");
        }
        if (req.getAmount().compareTo(p.getPendingAmount()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "付款金额不可超过待付金额，当前待付：" + p.getPendingAmount());
        }

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

        BigDecimal newPaid = p.getPaidAmount().add(req.getAmount());
        BigDecimal newPending = p.getTotalAmount().subtract(newPaid);
        p.setPaidAmount(newPaid);
        p.setPendingAmount(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
        p.setStatus(newPending.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_PARTIAL);
        payableMapper.updateById(p);
    }

    // ===================== 汇总 =====================

    public FinDtos.PayableSummary summary(Long supplierId, String month) {
        Long tenantId = requireTenantId();
        FinDtos.PayableSummary s = new FinDtos.PayableSummary();

        LambdaQueryWrapper<FinPayable> w = new LambdaQueryWrapper<FinPayable>()
                .eq(FinPayable::getTenantId, tenantId)
                .eq(FinPayable::getDelFlag, 0)
                .eq(supplierId != null, FinPayable::getSupplierId, supplierId);

        List<FinPayable> list = payableMapper.selectList(w);

        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long unpaid = 0, partPaid = 0, settled = 0;

        for (FinPayable p : list) {
            totalPayable = totalPayable.add(p.getTotalAmount());
            totalPaid = totalPaid.add(p.getPaidAmount());
            totalPending = totalPending.add(p.getPendingAmount());
            if (p.getStatus() == STATUS_UNPAID) unpaid++;
            else if (p.getStatus() == STATUS_PARTIAL) partPaid++;
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

    // ===================== 私有方法 =====================

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
