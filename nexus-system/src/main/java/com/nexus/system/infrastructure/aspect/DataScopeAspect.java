package com.nexus.system.infrastructure.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.context.DataScopeContext;
import com.nexus.common.context.GatewayUserContext;
import com.nexus.common.context.OrgContext;
import com.nexus.common.security.datascope.DataScope;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final SysUserMapper sysUserMapper;
    private final SysUserShopRoleMapper sysUserShopRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    @Before("@annotation(dataScopeAnnotation)")
    public void doBefore(JoinPoint point, com.nexus.common.annotation.DataScope dataScopeAnnotation) {
        clearDataScope();

        Long userId = GatewayUserContext.getUserId();
        if (userId == null) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }

        DataScopeContext.setUserId(userId);
        DataScopeContext.setDeptId(user.getMainOrgId());

        LambdaQueryWrapper<SysUserShopRole> usrQuery = new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getDelFlag, 0);
        Long currentShopId = OrgContext.getShopId();
        if (currentShopId != null) {
            usrQuery.eq(SysUserShopRole::getShopId, currentShopId);
        }
        List<SysUserShopRole> mappings = sysUserShopRoleMapper.selectList(usrQuery);

        if (mappings.isEmpty()) {
            DataScopeContext.setDataScope(DataScope.SELF.getCode());
            return;
        }

        List<DataScope> scopes = new ArrayList<>();
        for (SysUserShopRole m : mappings) {
            SysRole role = sysRoleMapper.selectById(m.getRoleId());
            if (role != null && role.getDataScope() != null
                    && (role.getDelFlag() == null || role.getDelFlag() == 0)) {
                DataScope scope = DataScope.fromCode(role.getDataScope());
                if (scope != null) {
                    scopes.add(scope);
                }
            }
        }

        DataScope finalScope = getMaxDataScope(scopes);
        DataScopeContext.setDataScope(finalScope.getCode());
    }

    @After("@annotation(dataScopeAnnotation)")
    public void doAfter(JoinPoint point, com.nexus.common.annotation.DataScope dataScopeAnnotation) {
        clearDataScope();
    }

    private void clearDataScope() {
        DataScopeContext.clear();
    }

    private DataScope getMaxDataScope(List<DataScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return DataScope.SELF;
        }
        DataScope max = DataScope.SELF;
        for (DataScope scope : scopes) {
            if (scope == null) {
                continue;
            }
            if (scope.getCode() > max.getCode()) {
                max = scope;
            }
        }
        return max;
    }
}
