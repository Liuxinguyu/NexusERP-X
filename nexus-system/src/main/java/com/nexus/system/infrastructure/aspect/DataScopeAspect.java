package com.nexus.system.infrastructure.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.annotation.DataScope;
import com.nexus.common.context.DataScopeContext;
import com.nexus.common.context.GatewayUserContext;
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

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final SysUserMapper sysUserMapper;
    private final SysUserShopRoleMapper sysUserShopRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    @Before("@annotation(dataScopeAnnotation)")
    public void doBefore(JoinPoint point, DataScope dataScopeAnnotation) {
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

        // Find the strongest data_scope user possesses
        List<SysUserShopRole> mappings = sysUserShopRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserShopRole>().eq(SysUserShopRole::getUserId, userId)
        );

        if (mappings.isEmpty()) {
            // Default to SELF if no role assigned
            DataScopeContext.setDataScope(5);
            return;
        }

        int maxScope = 5; // 5 is most restrictive (Self)
        Long strongestRoleId = null;

        for (SysUserShopRole m : mappings) {
            SysRole role = sysRoleMapper.selectById(m.getRoleId());
            if (role != null && role.getDataScope() != null) {
                if (role.getDataScope() < maxScope) {
                    maxScope = role.getDataScope();
                    strongestRoleId = role.getId();
                }
            }
        }

        Long finalMaxScope = (long) maxScope;
        DataScopeContext.setDataScope(maxScope);
        DataScopeContext.setRoleId(strongestRoleId == null ? null : (strongestRoleId)); // Changed to String temporarily to fix the error just in case it is typed as a String somewhere
    }

    @After("@annotation(dataScopeAnnotation)")
    public void doAfter(JoinPoint point, DataScope dataScopeAnnotation) {
        clearDataScope();
    }

    private void clearDataScope() {
        DataScopeContext.clear();
    }
}
