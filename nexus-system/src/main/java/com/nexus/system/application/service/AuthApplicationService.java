package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.SecurityUtils;
import com.nexus.common.security.datascope.DataScope;
import com.nexus.common.security.jwt.JwtTokenProvider;
import com.nexus.system.application.dto.AuthDtos;
import com.nexus.system.domain.model.SysMenu;
import com.nexus.system.domain.model.SysRole;
import com.nexus.system.domain.model.SysRoleMenu;
import com.nexus.system.domain.model.SysShop;
import com.nexus.system.domain.model.SysUser;
import com.nexus.system.domain.model.SysUserShopRole;
import com.nexus.system.infrastructure.mapper.SysMenuMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMapper;
import com.nexus.system.infrastructure.mapper.SysRoleMenuMapper;
import com.nexus.system.infrastructure.mapper.SysShopMapper;
import com.nexus.system.infrastructure.mapper.SysUserMapper;
import com.nexus.system.infrastructure.mapper.SysUserShopRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserShopRoleMapper userShopRoleMapper;
    private final SysShopMapper shopMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final SysOrgApplicationService sysOrgApplicationService;
    private final LoginCaptchaValidator loginCaptchaValidator;
    private final SysLoginLogApplicationService sysLoginLogApplicationService;
    private final OnlineUserRedisService onlineUserRedisService;

    public AuthApplicationService(SysUserMapper userMapper,
                                  SysUserShopRoleMapper userShopRoleMapper,
                                  SysShopMapper shopMapper,
                                  SysRoleMapper roleMapper,
                                  SysRoleMenuMapper roleMenuMapper,
                                  SysMenuMapper menuMapper,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider,
                                  AuthRedisService authRedisService,
                                  SysOrgApplicationService sysOrgApplicationService,
                                  LoginCaptchaValidator loginCaptchaValidator,
                                  SysLoginLogApplicationService sysLoginLogApplicationService,
                                  OnlineUserRedisService onlineUserRedisService) {
        this.userMapper = userMapper;
        this.userShopRoleMapper = userShopRoleMapper;
        this.shopMapper = shopMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authRedisService = authRedisService;
        this.sysOrgApplicationService = sysOrgApplicationService;
        this.loginCaptchaValidator = loginCaptchaValidator;
        this.sysLoginLogApplicationService = sysLoginLogApplicationService;
        this.onlineUserRedisService = onlineUserRedisService;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, HttpServletRequest http) {
        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");
        String nameForLog = request != null && StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : "";

        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            sysLoginLogApplicationService.recordFailure(null, nameForLog.isEmpty() ? "unknown" : nameForLog, ip, ua, "用户名或密码为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码不能为空");
        }
        LambdaQueryWrapper<SysUser> uq = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim())
                .eq(SysUser::getDelFlag, 0);
        if (request.getTenantId() != null) {
            uq.eq(SysUser::getTenantId, request.getTenantId());
        }
        SysUser user = userMapper.selectOne(uq.last("limit 1"));
        if (user == null) {
            sysLoginLogApplicationService.recordFailure(null, nameForLog, ip, ua, "用户名或密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        try {
            loginCaptchaValidator.validate(user.getTenantId(), request.getCaptchaKey(), request.getCaptcha());
        } catch (BusinessException ex) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, ex.getMessage());
            throw ex;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "账号已停用");
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "用户名或密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        List<AuthDtos.ShopItem> shops = queryShops(user.getId());
        if (shops.isEmpty()) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "无可登录店铺");
            throw new BusinessException(ResultCode.FORBIDDEN, "当前用户无可登录店铺");
        }
        Long currentShopId = chooseCurrentShop(user.getMainShopId(), shops);
        ScopeData scope = scopeByUserAndShop(user.getTenantId(), user.getId(), currentShopId);

        authRedisService.saveUserShops(user.getId(), shops);
        authRedisService.saveSession(user.getId(),
                new AuthRedisService.SessionState(currentShopId, scope.dataScope, scope.accessibleShopIds));

        SysShop currentShop = shopMapper.selectById(currentShopId);
        Long currentOrgId = currentShop != null ? currentShop.getOrgId() : null;

        Collection<GrantedAuthority> authorities = buildAuthorities(user.getId(), currentShopId);

        NexusPrincipal principal = new NexusPrincipal(
                user.getId(), user.getUsername(), user.getTenantId(), currentShopId, currentOrgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String token = jwtTokenProvider.createAccessToken(principal);

        try {
            onlineUserRedisService.recordLogin(token, user, ip, ua);
        } catch (Exception e) {
            log.warn("在线会话写入 Redis 失败: {}", e.getMessage());
        }

        sysLoginLogApplicationService.recordSuccess(user.getTenantId(), user.getUsername(), ip, ua, "登录成功");

        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setAccessToken(token);
        resp.setTokenType("Bearer");
        resp.setTenantId(user.getTenantId());
        resp.setCurrentShopId(currentShopId);
        resp.setCurrentOrgId(currentOrgId);
        resp.setDataScope(scope.dataScope);
        resp.setAccessibleShopIds(scope.accessibleShopIds);
        resp.setAccessibleOrgIds(scope.accessibleOrgIds);
        return resp;
    }

    private static String clientIp(HttpServletRequest request) {
        String x = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(x)) {
            return x.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public List<AuthDtos.ShopItem> getShops(Long userId) {
        List<AuthDtos.ShopItem> shops = authRedisService.getUserShops(userId);
        if (shops.isEmpty()) {
            shops = queryShops(userId);
            if (!shops.isEmpty()) {
                authRedisService.saveUserShops(userId, shops);
            }
        } else {
            authRedisService.refreshTtl(userId);
        }
        return shops;
    }

    /**
     * Token 续期（Rotation）：基于当前登录态重新签发 token，旧 token 立即失效。
     * 前端在 access token 即将过期时调用此接口获取新 token。
     */
    public AuthDtos.LoginResponse refreshToken(String oldRawJwt, HttpServletRequest http) {
        var currentPrincipal = SecurityUtils.currentPrincipal();
        if (currentPrincipal == null || currentPrincipal.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }

        Long userId = currentPrincipal.getUserId();
        Long tenantId = currentPrincipal.getTenantId();
        String username = currentPrincipal.getUsername();
        Long currentShopId = currentPrincipal.getShopId();

        // 确认用户仍然存在且未停用
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDelFlag() != null && user.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已删除");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }

        // 确认当前店铺仍然有效
        if (currentShopId == null) {
            List<AuthDtos.ShopItem> shops = queryShops(userId);
            if (shops.isEmpty()) {
                throw new BusinessException(ResultCode.FORBIDDEN, "当前用户无可登录店铺");
            }
            currentShopId = chooseCurrentShop(user.getMainShopId(), shops);
        }

        ScopeData scope = scopeByUserAndShop(tenantId, userId, currentShopId);

        SysShop currentShop = shopMapper.selectById(currentShopId);
        Long currentOrgId = currentShop != null ? currentShop.getOrgId() : null;

        Collection<GrantedAuthority> authorities = buildAuthorities(userId, currentShopId);

        NexusPrincipal principal = new NexusPrincipal(
                userId, username, tenantId, currentShopId, currentOrgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String newToken = jwtTokenProvider.createAccessToken(principal);

        // 旧 token 失效（Rotation）
        if (StringUtils.hasText(oldRawJwt)) {
            onlineUserRedisService.removeToken(oldRawJwt);
        }

        // 新 token 写入在线会话
        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");
        try {
            onlineUserRedisService.recordLogin(newToken, user, ip, ua);
        } catch (Exception e) {
            log.warn("Token 续期后写入在线会话失败: {}", e.getMessage());
        }

        sysLoginLogApplicationService.recordSuccess(tenantId, username, ip, ua, "Token 续期");

        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setAccessToken(newToken);
        resp.setTokenType("Bearer");
        resp.setTenantId(tenantId);
        resp.setCurrentShopId(currentShopId);
        resp.setCurrentOrgId(currentOrgId);
        resp.setDataScope(scope.dataScope);
        resp.setAccessibleShopIds(scope.accessibleShopIds);
        resp.setAccessibleOrgIds(scope.accessibleOrgIds);
        return resp;
    }

    public AuthDtos.LoginResponse switchShop(Long userId, Long tenantId, String username, Long targetShopId,
                                             String oldRawJwt, HttpServletRequest http) {
        if (targetShopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "shopId 不能为空");
        }
        long exists = userShopRoleMapper.selectCount(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getShopId, targetShopId)
                .eq(SysUserShopRole::getDelFlag, 0));
        if (exists <= 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无该店铺权限，无法切换");
        }

        ScopeData scope = scopeByUserAndShop(tenantId, userId, targetShopId);
        authRedisService.saveSession(userId,
                new AuthRedisService.SessionState(targetShopId, scope.dataScope, scope.accessibleShopIds));

        SysShop targetShop = shopMapper.selectById(targetShopId);
        Long orgId = targetShop != null ? targetShop.getOrgId() : null;

        Collection<GrantedAuthority> authorities = buildAuthorities(userId, targetShopId);

        NexusPrincipal principal = new NexusPrincipal(
                userId, username, tenantId, targetShopId, orgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String token = jwtTokenProvider.createAccessToken(principal);

        if (StringUtils.hasText(oldRawJwt)) {
            onlineUserRedisService.removeToken(oldRawJwt);
        }
        SysUser user = userMapper.selectById(userId);
        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");
        if (user != null) {
            try {
                onlineUserRedisService.recordLogin(token, user, ip, ua);
            } catch (Exception e) {
                log.warn("切换店铺后写入在线会话失败: {}", e.getMessage());
            }
        }

        sysLoginLogApplicationService.recordSuccess(tenantId, username, ip, ua, "切换店铺");

        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setAccessToken(token);
        resp.setTokenType("Bearer");
        resp.setTenantId(tenantId);
        resp.setCurrentShopId(targetShopId);
        resp.setCurrentOrgId(orgId);
        resp.setDataScope(scope.dataScope);
        resp.setAccessibleShopIds(scope.accessibleShopIds);
        resp.setAccessibleOrgIds(scope.accessibleOrgIds);
        return resp;
    }

    private List<AuthDtos.ShopItem> queryShops(Long userId) {
        List<SysUserShopRole> mapping = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getDelFlag, 0));
        List<Long> shopIds = mapping.stream().map(SysUserShopRole::getShopId)
                .filter(Objects::nonNull).distinct().toList();
        if (shopIds.isEmpty()) {
            return List.of();
        }
        return shopMapper.selectList(new LambdaQueryWrapper<SysShop>()
                        .in(SysShop::getId, shopIds)
                        .eq(SysShop::getDelFlag, 0)
                        .eq(SysShop::getStatus, 1))
                .stream()
                .sorted(Comparator.comparing(SysShop::getId))
                .map(s -> {
                    AuthDtos.ShopItem item = new AuthDtos.ShopItem();
                    item.setShopId(s.getId());
                    item.setShopName(s.getShopName());
                    item.setShopType(s.getShopType());
                    item.setOrgId(s.getOrgId());
                    return item;
                }).collect(Collectors.toList());
    }

    private static Long chooseCurrentShop(Long mainShopId, List<AuthDtos.ShopItem> shops) {
        if (mainShopId != null) {
            for (AuthDtos.ShopItem s : shops) {
                if (mainShopId.equals(s.getShopId())) {
                    return mainShopId;
                }
            }
        }
        return shops.get(0).getShopId();
    }

    private ScopeData scopeByUserAndShop(Long tenantId, Long userId, Long shopId) {
        List<SysUserShopRole> mapping = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getShopId, shopId)
                .eq(SysUserShopRole::getDelFlag, 0));
        if (mapping.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前店铺无角色授权");
        }
        List<Long> roleIds = mapping.stream().map(SysUserShopRole::getRoleId).filter(Objects::nonNull).distinct().toList();
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getDelFlag, 0));
        if (roles.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "角色不存在或已删除");
        }

        int maxScope = roles.stream()
                .map(SysRole::getDataScope)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(2);

        List<Long> accessibleShopIds = List.of();
        List<Long> accessibleOrgIds = List.of();

        SysShop current = shopMapper.selectById(shopId);
        Long rootOrgId = current != null ? current.getOrgId() : null;

        if (maxScope == DataScope.ORG_AND_SUB_SHOPS.getCode() && tenantId != null && rootOrgId != null) {
            accessibleOrgIds = sysOrgApplicationService.listSelfAndDescendantOrgIds(tenantId, rootOrgId);
            accessibleShopIds = shopMapper.selectList(new LambdaQueryWrapper<SysShop>()
                            .eq(SysShop::getTenantId, tenantId)
                            .in(SysShop::getOrgId, accessibleOrgIds)
                            .eq(SysShop::getDelFlag, 0)
                            .eq(SysShop::getStatus, 1))
                    .stream().map(SysShop::getId).filter(Objects::nonNull).distinct().toList();
            if (accessibleShopIds.isEmpty()) {
                accessibleShopIds = List.of(shopId);
            }
            if (accessibleOrgIds.isEmpty()) {
                accessibleOrgIds = List.of(rootOrgId);
            }
        } else if (maxScope == 2 && rootOrgId != null) {
            accessibleOrgIds = List.of(rootOrgId);
        }

        return new ScopeData(maxScope, accessibleShopIds, accessibleOrgIds);
    }

    /**
     * 根据用户在指定店铺下的角色，构建 Spring Security GrantedAuthority 集合。
     * <p>
     * 包含两类权限：
     * <ul>
     *   <li>角色级别：ROLE_{roleCode}（如 ROLE_ADMIN、ROLE_CASHIER）</li>
     *   <li>菜单级别：sys_menu.perms（如 system:user:create）</li>
     * </ul>
     */
    private Collection<GrantedAuthority> buildAuthorities(Long userId, Long shopId) {
        // 1. 查询用户在该店铺下的角色绑定
        List<SysUserShopRole> mapping = userShopRoleMapper.selectList(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getShopId, shopId)
                .eq(SysUserShopRole::getDelFlag, 0));
        List<Long> roleIds = mapping.stream()
                .map(SysUserShopRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        // 2. 查询角色实体，提取 roleCode → ROLE_xxx
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getDelFlag, 0));
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (SysRole role : roles) {
            if (role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
                String code = role.getRoleCode().trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + code));
            }
        }

        // 3. 查询角色关联的菜单权限标识 → 如 system:user:create
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds));
        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!menuIds.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIds)
                    .eq(SysMenu::getDelFlag, 0));
            for (SysMenu menu : menus) {
                if (menu.getPerms() != null && !menu.getPerms().isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(menu.getPerms().trim()));
                }
            }
        }

        return authorities.stream().distinct().collect(Collectors.toList());
    }

    private record ScopeData(Integer dataScope, List<Long> accessibleShopIds, List<Long> accessibleOrgIds) {
    }
}
