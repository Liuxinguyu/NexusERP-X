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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinReceivableApplicationService {

    private static final String SOURCE_SALE_ORDER = "sale_order";
    private static final String SOURCE_CRM_CONTRACT = "crm_contract";
    private static final int STATUS_UNPAID = 0;      // 未回款
    private static final int STATUS_PARTIAL = 1;      // 部分回款
    private static final int STATUS_SETTLED = 2;    // 已结清

    private final FinReceivableMapper receivableMapper;
    private final FinReceivableRecordMapper recordMapper;
    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpCustomerMapper customerMapper;

    // ===================== 分页查询 =====================

    public IPage<FinDtos.ReceivableVO> page(long current, long size, FinDtos.ReceivablePageQuery query) {
        Long tenantId = requireTenantId();
        Page<FinReceivable> p = new Page<>(current, size);
        LambdaQueryWrapper<FinReceivable> w = new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getTenantId, tenantId)
                .eq(FinReceivable::getDelFlag, 0)
                .eq(query.getStatus() != null, FinReceivable::getStatus, query.getStatus())
                .eq(query.getCustomerId() != null, FinReceivable::getCustomerId, query.getCustomerId())
                .ge(query.getDateFrom() != null && !query.getDateFrom().isBlank(),
                        FinReceivable::getCreateTime, query.getDateFrom() + " 00:00:00")
                .le(query.getDateTo() != null && !query.getDateTo().isBlank(),
                        FinReceivable::getCreateTime, query.getDateTo() + " 23:59:59")
                .orderByDesc(FinReceivable::getId);
        IPage<FinReceivable> result = receivableMapper.selectPage(p, w);
        return result.convert(this::toVO);
    }

    public FinDtos.ReceivableVO getById(Long id) {
        Long tenantId = requireTenantId();
        FinReceivable r = loadReceivable(id, tenantId);
        return toVO(r);
    }

    public List<FinDtos.ReceivableRecordVO> listRecords(Long receivableId) {
        Long tenantId = requireTenantId();
        loadReceivable(receivableId, tenantId);
        return recordMapper.selectList(new LambdaQueryWrapper<FinReceivableRecord>()
                .eq(FinReceivableRecord::getTenantId, tenantId)
                .eq(FinReceivableRecord::getReceivableId, receivableId)
                .eq(FinReceivableRecord::getDelFlag, 0)
                .orderByDesc(FinReceivableRecord::getId))
                .stream().map(this::toRecordVO).toList();
    }

    // ===================== 创建应收 =====================

    @Transactional(rollbackFor = Exception.class)
    public Long create(FinDtos.ReceivableCreateRequest req) {
        Long tenantId = requireTenantId();
        FinReceivable r = new FinReceivable();
        r.setTenantId(tenantId);
        r.setReceivableNo(nextNo("RC"));
        r.setSourceType(StringUtils.hasText(req.getSourceType()) ? req.getSourceType() : SOURCE_SALE_ORDER);
        r.setSourceId(req.getSourceId() != null ? req.getSourceId() : 0L);
        r.setCustomerId(req.getCustomerId());
        r.setCustomerName(req.getCustomerName());
        r.setTotalAmount(req.getTotalAmount());
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setPendingAmount(req.getTotalAmount());
        r.setInvoiceNo(req.getInvoiceNo());
        r.setDueDate(req.getDueDate());
        r.setStatus(STATUS_UNPAID);
        r.setRemark(req.getRemark());
        receivableMapper.insert(r);
        return r.getId();
    }

    /**
     * 销售订单确认出库时，自动创建应收记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromSaleOrder(Long saleOrderId) {
        Long tenantId = requireTenantId();
        ErpSaleOrder order = loadSaleOrder(saleOrderId, tenantId);
        if (order.getCustomerId() == null) {
            return null; // 无客户不创建应收
        }
        ErpCustomer customer = customerMapper.selectById(order.getCustomerId());
        String customerName = customer != null ? customer.getName() : "";

        FinReceivable r = new FinReceivable();
        r.setTenantId(tenantId);
        r.setReceivableNo(nextNo("RC"));
        r.setSourceType(SOURCE_SALE_ORDER);
        r.setSourceId(order.getId());
        r.setCustomerId(order.getCustomerId());
        r.setCustomerName(customerName);
        r.setTotalAmount(order.getTotalAmount());
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setPendingAmount(order.getTotalAmount());
        r.setStatus(STATUS_UNPAID);
        receivableMapper.insert(r);
        return r.getId();
    }

    // ===================== 更新应收 =====================

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinDtos.ReceivableUpdateRequest req) {
        Long tenantId = requireTenantId();
        FinReceivable r = loadReceivable(id, tenantId);
        if (r.getStatus() == STATUS_SETTLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已结清的单据不可修改");
        }
        r.setInvoiceNo(req.getInvoiceNo());
        r.setDueDate(req.getDueDate());
        r.setRemark(req.getRemark());
        receivableMapper.updateById(r);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        FinReceivable r = loadReceivable(id, tenantId);
        if (r.getStatus() != STATUS_UNPAID || r.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已有收款记录的单据不可删除");
        }
        receivableMapper.deleteById(id);
    }

    // ===================== 登记收款 =====================

    @Transactional(rollbackFor = Exception.class)
    public void recordReceipt(Long receivableId, FinDtos.ReceivableRecordCreate req) {
        Long tenantId = requireTenantId();
        Long userId = SecurityUtils.currentUserId() != null ? SecurityUtils.currentUserId() : 0L;
        FinReceivable r = loadReceivable(receivableId, tenantId);

        if (r.getStatus() == STATUS_SETTLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该单据已结清，无需再收款");
        }
        if (req.getAmount().compareTo(r.getPendingAmount()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "收款金额不可超过待收金额，当前待收：" + r.getPendingAmount());
        }

        // 插入收款记录
        FinReceivableRecord record = new FinReceivableRecord();
        record.setTenantId(tenantId);
        record.setReceivableId(receivableId);
        record.setRecordNo(nextNo("RR"));
        record.setAmount(req.getAmount());
        record.setPaymentMethod(req.getPaymentMethod());
        record.setPaymentAccount(req.getPaymentAccount());
        record.setPaymentTime(req.getPaymentTime() != null ? req.getPaymentTime() : LocalDateTime.now());
        record.setHandlerUserId(userId);
        record.setReceiptUrl(req.getReceiptUrl());
        record.setRemark(req.getRemark());
        recordMapper.insert(record);

        // 更新应收余额
        BigDecimal newReceived = r.getReceivedAmount().add(req.getAmount());
        BigDecimal newPending = r.getTotalAmount().subtract(newReceived);
        r.setReceivedAmount(newReceived);
        r.setPendingAmount(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
        r.setStatus(newPending.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_PARTIAL);
        receivableMapper.updateById(r);
    }

    // ===================== 汇总 =====================

    public FinDtos.ReceivableSummary summary(Long customerId, String month) {
        Long tenantId = requireTenantId();
        FinDtos.ReceivableSummary s = new FinDtos.ReceivableSummary();

        LambdaQueryWrapper<FinReceivable> w = new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getTenantId, tenantId)
                .eq(FinReceivable::getDelFlag, 0)
                .eq(customerId != null, FinReceivable::getCustomerId, customerId);

        List<FinReceivable> list = receivableMapper.selectList(w);

        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long unpaid = 0, partPaid = 0, settled = 0;

        for (FinReceivable r : list) {
            totalReceivable = totalReceivable.add(r.getTotalAmount());
            totalReceived = totalReceived.add(r.getReceivedAmount());
            totalPending = totalPending.add(r.getPendingAmount());
            if (r.getStatus() == STATUS_UNPAID) unpaid++;
            else if (r.getStatus() == STATUS_PARTIAL) partPaid++;
            else settled++;
        }
        s.setTotalReceivable(totalReceivable);
        s.setTotalReceived(totalReceived);
        s.setTotalPending(totalPending);
        s.setUnpaidCount(unpaid);
        s.setPartPaidCount(partPaid);
        s.setSettledCount(settled);
        return s;
    }

    // ===================== 私有方法 =====================

    private FinReceivable loadReceivable(Long id, Long tenantId) {
        FinReceivable r = receivableMapper.selectById(id);
        if (r == null || !Objects.equals(r.getTenantId(), tenantId)
                || (r.getDelFlag() != null && r.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "应收单不存在");
        }
        return r;
    }

    private ErpSaleOrder loadSaleOrder(Long id, Long tenantId) {
        ErpSaleOrder o = saleOrderMapper.selectById(id);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId)
                || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "销售单不存在");
        }
        return o;
    }

    private FinDtos.ReceivableVO toVO(FinReceivable r) {
        FinDtos.ReceivableVO vo = new FinDtos.ReceivableVO();
        vo.setId(r.getId());
        vo.setReceivableNo(r.getReceivableNo());
        vo.setSourceType(r.getSourceType());
        vo.setSourceId(r.getSourceId());
        vo.setCustomerId(r.getCustomerId());
        vo.setCustomerName(r.getCustomerName());
        vo.setTotalAmount(r.getTotalAmount());
        vo.setReceivedAmount(r.getReceivedAmount());
        vo.setPendingAmount(r.getPendingAmount());
        vo.setInvoiceNo(r.getInvoiceNo());
        vo.setDueDate(r.getDueDate());
        vo.setStatus(r.getStatus());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private FinDtos.ReceivableRecordVO toRecordVO(FinReceivableRecord r) {
        FinDtos.ReceivableRecordVO vo = new FinDtos.ReceivableRecordVO();
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
