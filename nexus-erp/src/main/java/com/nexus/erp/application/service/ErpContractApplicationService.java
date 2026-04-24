package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.support.DateParsers;
import com.nexus.erp.domain.model.ErpContract;
import com.nexus.erp.domain.model.ErpContractItem;
import com.nexus.erp.domain.model.ErpCustomer;
import com.nexus.erp.infrastructure.mapper.ErpContractItemMapper;
import com.nexus.erp.infrastructure.mapper.ErpContractMapper;
import com.nexus.erp.infrastructure.mapper.ErpCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ErpContractApplicationService {

    private static final int STATUS_ONGOING = 1;
    private static final int STATUS_EXPIRED = 2;
    private static final int STATUS_TERMINATED = 3;

    private final ErpContractMapper contractMapper;
    private final ErpContractItemMapper contractItemMapper;
    private final ErpCustomerMapper customerMapper;

    public IPage<ErpDtos.ContractVO> page(long current, long size,
                                          Long customerId, Integer status,
                                          String dateFrom, String dateTo) {
        Long tenantId = requireTenantId();
        Page<ErpContract> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpContract> w = new LambdaQueryWrapper<ErpContract>()
                .eq(ErpContract::getTenantId, tenantId)
                .eq(ErpContract::getDelFlag, 0)
                .eq(customerId != null, ErpContract::getCustomerId, customerId)
                .eq(status != null, ErpContract::getStatus, status)
                .ge(dateFrom != null && !dateFrom.isBlank(), ErpContract::getSignDate, dateFrom)
                .le(dateTo != null && !dateTo.isBlank(), ErpContract::getSignDate, dateTo)
                .orderByDesc(ErpContract::getId);
        IPage<ErpContract> result = contractMapper.selectPage(p, w);
        return result.convert(this::toVO);
    }

    public ErpDtos.ContractVO getById(Long id) {
        Long tenantId = requireTenantId();
        return toVO(loadContract(id, tenantId));
    }

    public List<ErpDtos.ContractItemVO> listItems(Long contractId) {
        Long tenantId = requireTenantId();
        loadContract(contractId, tenantId);
        return contractItemMapper.selectList(new LambdaQueryWrapper<ErpContractItem>()
                .eq(ErpContractItem::getTenantId, tenantId)
                .eq(ErpContractItem::getContractId, contractId)
                .eq(ErpContractItem::getDelFlag, 0))
                .stream().map(this::toItemVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.ContractCreateRequest req) {
        Long tenantId = requireTenantId();
        ErpCustomer customer = ensureCustomer(tenantId, req.getCustomerId());

        ErpContract c = new ErpContract();
        c.setTenantId(tenantId);
        c.setContractNo(nextContractNo());
        c.setCustomerId(req.getCustomerId());
        c.setCustomerName(customer.getName());
        c.setContractName(req.getContractName().trim());
        c.setOpportunityId(req.getOpportunityId());
        c.setAmount(req.getAmount());
        c.setSignedBy(req.getSignedBy());
        c.setAttachmentUrls(req.getAttachmentUrls());
        c.setRemark(req.getRemark());
        c.setStatus(STATUS_ONGOING);

        if (req.getSignDate() != null && !req.getSignDate().isBlank()) {
            c.setSignDate(DateParsers.parseIsoDate(req.getSignDate(), "签订日期"));
        }
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            c.setStartDate(DateParsers.parseIsoDate(req.getStartDate(), "开始日期"));
        }
        if (req.getEndDate() != null && !req.getEndDate().isBlank()) {
            c.setEndDate(DateParsers.parseIsoDate(req.getEndDate(), "结束日期"));
        }
        contractMapper.insert(c);
        return c.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.ContractUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpContract c = loadContract(id, tenantId);
        if (req.getContractName() != null && !req.getContractName().isBlank()) {
            c.setContractName(req.getContractName().trim());
        }
        if (req.getOpportunityId() != null) {
            c.setOpportunityId(req.getOpportunityId());
        }
        if (req.getSignDate() != null && !req.getSignDate().isBlank()) {
            c.setSignDate(DateParsers.parseIsoDate(req.getSignDate(), "签订日期"));
        }
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            c.setStartDate(DateParsers.parseIsoDate(req.getStartDate(), "开始日期"));
        }
        if (req.getEndDate() != null && !req.getEndDate().isBlank()) {
            c.setEndDate(DateParsers.parseIsoDate(req.getEndDate(), "结束日期"));
        }
        if (req.getAmount() != null) {
            c.setAmount(req.getAmount());
        }
        if (req.getSignedBy() != null) {
            c.setSignedBy(req.getSignedBy());
        }
        if (req.getAttachmentUrls() != null) {
            c.setAttachmentUrls(req.getAttachmentUrls());
        }
        if (req.getStatus() != null) {
            c.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            c.setRemark(req.getRemark());
        }
        contractMapper.updateById(c);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        loadContract(id, tenantId);
        // 删除明细
        contractItemMapper.delete(new LambdaQueryWrapper<ErpContractItem>()
                .eq(ErpContractItem::getContractId, id)
                .eq(ErpContractItem::getTenantId, tenantId)
                .eq(ErpContractItem::getDelFlag, 0));
        contractMapper.deleteById(id);
    }

    private ErpContract loadContract(Long id, Long tenantId) {
        ErpContract c = contractMapper.selectById(id);
        if (c == null || !Objects.equals(c.getTenantId(), tenantId)
                || (c.getDelFlag() != null && c.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "合同不存在");
        }
        return c;
    }

    private ErpCustomer ensureCustomer(Long tenantId, Long customerId) {
        ErpCustomer c = customerMapper.selectById(customerId);
        if (c == null || !Objects.equals(c.getTenantId(), tenantId)
                || (c.getDelFlag() != null && c.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "客户不存在");
        }
        return c;
    }

    private static String nextContractNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(28);
        return "CT" + ts + uuidPart;
    }

    private ErpDtos.ContractVO toVO(ErpContract c) {
        ErpDtos.ContractVO vo = new ErpDtos.ContractVO();
        vo.setId(c.getId());
        vo.setContractNo(c.getContractNo());
        vo.setContractName(c.getContractName());
        vo.setCustomerId(c.getCustomerId());
        vo.setCustomerName(c.getCustomerName());
        vo.setOpportunityId(c.getOpportunityId());
        vo.setSignDate(c.getSignDate() != null ? c.getSignDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setStartDate(c.getStartDate() != null ? c.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setEndDate(c.getEndDate() != null ? c.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setAmount(c.getAmount());
        vo.setSignedBy(c.getSignedBy());
        vo.setAttachmentUrls(c.getAttachmentUrls());
        vo.setStatus(c.getStatus());
        vo.setRemark(c.getRemark());
        vo.setCreateTime(c.getCreateTime() != null
                ? c.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return vo;
    }

    private ErpDtos.ContractItemVO toItemVO(ErpContractItem it) {
        ErpDtos.ContractItemVO vo = new ErpDtos.ContractItemVO();
        vo.setId(it.getId());
        vo.setProductId(it.getProductId());
        vo.setProductName(it.getProductName());
        vo.setQuantity(it.getQuantity());
        vo.setUnitPrice(it.getUnitPrice());
        vo.setSubtotal(it.getSubtotal());
        return vo;
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
