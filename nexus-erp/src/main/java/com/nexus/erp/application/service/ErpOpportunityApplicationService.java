package com.nexus.erp.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.erp.application.dto.ErpDtos;
import com.nexus.erp.application.support.DateParsers;
import com.nexus.erp.domain.model.ErpCustomer;
import com.nexus.erp.domain.model.ErpOpportunity;
import com.nexus.erp.infrastructure.mapper.ErpCustomerMapper;
import com.nexus.erp.infrastructure.mapper.ErpOpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErpOpportunityApplicationService {

    // 商机阶段常量
    private static final String STAGE_CLUE = "线索";
    private static final String STAGE_DEMAND = "需求确认";
    private static final String STAGE_PROPOSAL = "方案";
    private static final String STAGE_QUOTE = "报价";
    private static final String STAGE_WIN = "成交";
    private static final String STAGE_LOSE = "失败";

    // 阶段对应的固定赢单概率
    private static final Map<String, Integer> STAGE_PROBABILITY = Map.of(
            STAGE_CLUE, 10,
            STAGE_DEMAND, 30,
            STAGE_PROPOSAL, 60,
            STAGE_QUOTE, 80,
            STAGE_WIN, 100
    );

    private final ErpOpportunityMapper opportunityMapper;
    private final ErpCustomerMapper customerMapper;

    public IPage<ErpDtos.OpportunityVO> page(long current, long size,
                                             Long customerId, String stage, Integer status) {
        Long tenantId = requireTenantId();
        Page<ErpOpportunity> p = new Page<>(current, size);
        LambdaQueryWrapper<ErpOpportunity> w = new LambdaQueryWrapper<ErpOpportunity>()
                .eq(ErpOpportunity::getTenantId, tenantId)
                .eq(ErpOpportunity::getDelFlag, 0)
                .eq(customerId != null, ErpOpportunity::getCustomerId, customerId)
                .eq(stage != null && !stage.isBlank(), ErpOpportunity::getStage, stage)
                .eq(status != null, ErpOpportunity::getStatus, status)
                .orderByDesc(ErpOpportunity::getId);
        IPage<ErpOpportunity> result = opportunityMapper.selectPage(p, w);
        return result.convert(this::toVO);
    }

    public ErpDtos.OpportunityVO getById(Long id) {
        Long tenantId = requireTenantId();
        return toVO(loadOpportunity(id, tenantId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ErpDtos.OpportunityCreateRequest req) {
        Long tenantId = requireTenantId();
        ErpCustomer customer = ensureCustomer(tenantId, req.getCustomerId());

        ErpOpportunity opp = new ErpOpportunity();
        opp.setTenantId(tenantId);
        opp.setCustomerId(req.getCustomerId());
        opp.setCustomerName(customer.getName());
        opp.setOpportunityName(req.getOpportunityName().trim());
        opp.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        String stage = req.getStage() != null && !req.getStage().isBlank() ? req.getStage() : STAGE_CLUE;
        opp.setStage(stage);
        opp.setProbability(getProbability(stage));
        if (req.getExpectCloseDate() != null && !req.getExpectCloseDate().isBlank()) {
            opp.setExpectCloseDate(DateParsers.parseIsoDate(req.getExpectCloseDate(), "预计成交日期"));
        }
        opp.setOwnerUserId(req.getOwnerUserId());
        opp.setRemark(req.getRemark());
        opp.setStatus(1); // 进行中
        opportunityMapper.insert(opp);
        return opp.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ErpDtos.OpportunityUpdateRequest req) {
        Long tenantId = requireTenantId();
        ErpOpportunity opp = loadOpportunity(id, tenantId);
        if (Objects.equals(opp.getStatus(), 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已关闭的商机不可修改");
        }
        if (req.getOpportunityName() != null && !req.getOpportunityName().isBlank()) {
            opp.setOpportunityName(req.getOpportunityName().trim());
        }
        if (req.getAmount() != null) {
            opp.setAmount(req.getAmount());
        }
        if (req.getStage() != null && !req.getStage().isBlank()) {
            opp.setStage(req.getStage());
            opp.setProbability(getProbability(req.getStage()));
        }
        if (req.getProbability() != null) {
            opp.setProbability(req.getProbability());
        }
        if (req.getExpectCloseDate() != null && !req.getExpectCloseDate().isBlank()) {
            opp.setExpectCloseDate(DateParsers.parseIsoDate(req.getExpectCloseDate(), "预计成交日期"));
        }
        if (req.getOwnerUserId() != null) {
            opp.setOwnerUserId(req.getOwnerUserId());
        }
        if (req.getRemark() != null) {
            opp.setRemark(req.getRemark());
        }
        opportunityMapper.updateById(opp);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        loadOpportunity(id, tenantId);
        opportunityMapper.deleteById(id);
    }

    /**
     * 推进商机阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public void advanceStage(Long id, String nextStage) {
        Long tenantId = requireTenantId();
        ErpOpportunity opp = loadOpportunity(id, tenantId);
        if (Objects.equals(opp.getStatus(), 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已关闭的商机不可推进");
        }
        if (STAGE_WIN.equals(nextStage) || STAGE_LOSE.equals(nextStage)) {
            opp.setStatus(0); // 关闭
        }
        opp.setStage(nextStage);
        opp.setProbability(getProbability(nextStage));
        opportunityMapper.updateById(opp);
    }

    private ErpOpportunity loadOpportunity(Long id, Long tenantId) {
        ErpOpportunity o = opportunityMapper.selectById(id);
        if (o == null || !Objects.equals(o.getTenantId(), tenantId)
                || (o.getDelFlag() != null && o.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商机不存在");
        }
        return o;
    }

    private ErpCustomer ensureCustomer(Long tenantId, Long customerId) {
        ErpCustomer c = customerMapper.selectById(customerId);
        if (c == null || !Objects.equals(c.getTenantId(), tenantId)
                || (c.getDelFlag() != null && c.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "客户不存在");
        }
        return c;
    }

    private int getProbability(String stage) {
        return STAGE_PROBABILITY.getOrDefault(stage, 0);
    }

    private ErpDtos.OpportunityVO toVO(ErpOpportunity o) {
        ErpDtos.OpportunityVO vo = new ErpDtos.OpportunityVO();
        vo.setId(o.getId());
        vo.setCustomerId(o.getCustomerId());
        vo.setCustomerName(o.getCustomerName());
        vo.setOpportunityName(o.getOpportunityName());
        vo.setAmount(o.getAmount());
        vo.setStage(o.getStage());
        vo.setProbability(o.getProbability());
        vo.setExpectCloseDate(o.getExpectCloseDate() != null
                ? o.getExpectCloseDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setOwnerUserId(o.getOwnerUserId());
        vo.setRemark(o.getRemark());
        vo.setStatus(o.getStatus());
        vo.setCreateTime(o.getCreateTime() != null
                ? o.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
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
