package com.nexus.oa.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.oa.application.dto.OaDtos;
import com.nexus.oa.domain.model.OaEmployee;
import com.nexus.oa.infrastructure.mapper.OaEmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class OaEmployeeApplicationService {

    private final OaEmployeeMapper employeeMapper;

    public OaEmployeeApplicationService(OaEmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    public IPage<EmployeeVO> page(long current, long size, String name, String empNo) {
        Long tenantId = requireTenantId();
        Page<OaEmployee> p = new Page<>(current, size);
        LambdaQueryWrapper<OaEmployee> w = new LambdaQueryWrapper<OaEmployee>()
                .eq(OaEmployee::getTenantId, tenantId)
                .eq(OaEmployee::getDelFlag, 0)
                .like(StringUtils.hasText(name), OaEmployee::getName, name)
                .like(StringUtils.hasText(empNo), OaEmployee::getEmpNo, empNo)
                .orderByDesc(OaEmployee::getId);
        return employeeMapper.selectPage(p, w).convert(this::toVO);
    }

    public EmployeeVO getById(Long id) {
        Long tenantId = requireTenantId();
        OaEmployee e = employeeMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId) || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        return toVO(e);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(OaDtos.EmployeeCreateRequest req) {
        Long tenantId = requireTenantId();
        assertEmpNoUnique(tenantId, req.getEmpNo().trim(), null);
        OaEmployee e = new OaEmployee();
        e.setTenantId(tenantId);
        fillFromCreate(e, req);
        employeeMapper.insert(e);
        return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OaDtos.EmployeeUpdateRequest req) {
        Long tenantId = requireTenantId();
        OaEmployee exist = loadEmployee(id, tenantId);
        assertEmpNoUnique(tenantId, req.getEmpNo().trim(), id);
        fillFromUpdate(exist, req);
        employeeMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        OaEmployee exist = loadEmployee(id, tenantId);
        employeeMapper.deleteById(exist.getId());
    }

    private void fillFromCreate(OaEmployee e, OaDtos.EmployeeCreateRequest req) {
        e.setEmpNo(req.getEmpNo().trim());
        e.setName(req.getName().trim());
        e.setDept(StringUtils.hasText(req.getDept()) ? req.getDept().trim() : null);
        e.setPosition(StringUtils.hasText(req.getPosition()) ? req.getPosition().trim() : null);
        e.setHireDate(req.getHireDate());
        e.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null);
        e.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        e.setUserId(req.getUserId());
        e.setDirectLeaderUserId(req.getDirectLeaderUserId());
    }

    private void fillFromUpdate(OaEmployee e, OaDtos.EmployeeUpdateRequest req) {
        e.setEmpNo(req.getEmpNo().trim());
        e.setName(req.getName().trim());
        e.setDept(StringUtils.hasText(req.getDept()) ? req.getDept().trim() : null);
        e.setPosition(StringUtils.hasText(req.getPosition()) ? req.getPosition().trim() : null);
        e.setHireDate(req.getHireDate());
        e.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone().trim() : null);
        if (req.getStatus() != null) {
            e.setStatus(req.getStatus());
        }
        e.setUserId(req.getUserId());
        e.setDirectLeaderUserId(req.getDirectLeaderUserId());
    }

    private void assertEmpNoUnique(Long tenantId, String empNo, Long excludeId) {
        long cnt = employeeMapper.selectCount(new LambdaQueryWrapper<OaEmployee>()
                .eq(OaEmployee::getTenantId, tenantId)
                .eq(OaEmployee::getEmpNo, empNo)
                .eq(OaEmployee::getDelFlag, 0)
                .ne(excludeId != null, OaEmployee::getId, excludeId));
        if (cnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "工号已存在");
        }
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }

    private OaEmployee loadEmployee(Long id, Long tenantId) {
        OaEmployee e = employeeMapper.selectById(id);
        if (e == null || !Objects.equals(e.getTenantId(), tenantId)
                || (e.getDelFlag() != null && e.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        return e;
    }

    private EmployeeVO toVO(OaEmployee e) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(e.getId());
        vo.setEmpNo(e.getEmpNo());
        vo.setName(e.getName());
        vo.setDept(e.getDept());
        vo.setPosition(e.getPosition());
        vo.setHireDate(e.getHireDate() != null ? e.getHireDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        vo.setPhone(e.getPhone());
        vo.setStatus(e.getStatus());
        vo.setUserId(e.getUserId());
        vo.setDirectLeaderUserId(e.getDirectLeaderUserId());
        return vo;
    }

    @lombok.Data
    public static class EmployeeVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String empNo;
        private String name;
        private String dept;
        private String position;
        private String hireDate;
        private String phone;
        private Integer status;
        private Long userId;
        private Long directLeaderUserId;
    }
}
