package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.system.application.dto.SystemAdminDtos;
import com.nexus.system.domain.model.SysPost;
import com.nexus.system.infrastructure.mapper.SysPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysPostApplicationService {

    private final SysPostMapper sysPostMapper;

    public IPage<SysPost> page(long current, long size, String postCode, String postName) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysPost> qw = new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, tenantId)
                .eq(SysPost::getDelFlag, 0);
        if (StringUtils.hasText(postCode)) {
            qw.like(SysPost::getPostCode, postCode);
        }
        if (StringUtils.hasText(postName)) {
            qw.like(SysPost::getPostName, postName);
        }
        qw.orderByAsc(SysPost::getId);
        return sysPostMapper.selectPage(new Page<>(current, size), qw);
    }

    public List<SysPost> listOptions() {
        Long tenantId = requireTenantId();
        return sysPostMapper.selectList(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, tenantId)
                .eq(SysPost::getDelFlag, 0)
                .eq(SysPost::getStatus, 1)
                .orderByAsc(SysPost::getId));
    }

    public SysPost getByIdForCurrentTenant(Long id) {
        Long tenantId = requireTenantId();
        SysPost row = sysPostMapper.selectById(id);
        if (row == null || !Objects.equals(row.getTenantId(), tenantId)
                || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return row;
    }

    public Long create(SystemAdminDtos.PostCreateRequest req) {
        Long tenantId = requireTenantId();
        long exists = sysPostMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, tenantId)
                .eq(SysPost::getPostCode, req.getPostCode())
                .eq(SysPost::getDelFlag, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "岗位编码已存在");
        }
        SysPost entity = new SysPost();
        entity.setTenantId(tenantId);
        entity.setPostCode(req.getPostCode());
        entity.setPostName(req.getPostName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setDelFlag(0);
        sysPostMapper.insert(entity);
        return entity.getId();
    }

    public void update(Long id, SystemAdminDtos.PostUpdateRequest req) {
        Long tenantId = requireTenantId();
        SysPost exist = sysPostMapper.selectById(id);
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId)
                || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        if (StringUtils.hasText(req.getPostCode())) {
            long dup = sysPostMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                    .eq(SysPost::getTenantId, tenantId)
                    .eq(SysPost::getPostCode, req.getPostCode())
                    .ne(SysPost::getId, id)
                    .eq(SysPost::getDelFlag, 0));
            if (dup > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "岗位编码已存在");
            }
            exist.setPostCode(req.getPostCode());
        }
        if (req.getPostName() != null) exist.setPostName(req.getPostName());
        if (req.getStatus() != null) exist.setStatus(req.getStatus());
        sysPostMapper.updateById(exist);
    }

    public void delete(Long id) {
        Long tenantId = requireTenantId();
        SysPost exist = sysPostMapper.selectById(id);
        if (exist == null || !Objects.equals(exist.getTenantId(), tenantId)
                || (exist.getDelFlag() != null && exist.getDelFlag() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        exist.setDelFlag(1);
        sysPostMapper.updateById(exist);
    }

    private static Long requireTenantId() {
        Long tid = TenantContext.getTenantId();
        if (tid == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少租户上下文");
        }
        return tid;
    }
}
