package com.nexus.system.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.common.context.TenantContext;
import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;
import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.SecurityUtils;
import com.nexus.common.security.datascope.DataScope;
import com.nexus.common.security.jwt.JwtTokenProvider;
import com.nexus.common.utils.HttpRequestUtils;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthApplicationService {
    private static final int LOGIN_FAIL_MAX_TIMES = 5;
    private static final Duration LOGIN_FAIL_LOCK_TTL = Duration.ofMinutes(15);

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
    private final StringRedisTemplate stringRedisTemplate;

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
                                  OnlineUserRedisService onlineUserRedisService,
                                  StringRedisTemplate stringRedisTemplate) {
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
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public AuthDtos.PreAuthLoginResponse login(AuthDtos.LoginRequest request, HttpServletRequest http) {
        String ip = HttpRequestUtils.clientIp(http);
        String ua = http.getHeader("User-Agent");
        String nameForLog = request != null && StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : "";

        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            sysLoginLogApplicationService.recordFailure(null, nameForLog.isEmpty() ? "unknown" : nameForLog, ip, ua, "用户名或密码为空");
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码不能为空");
        }
        if (request.getTenantId() == null) {
            sysLoginLogApplicationService.recordFailure(null, nameForLog, ip, ua, "缺少租户标识");
            throw new BusinessException(ResultCode.BAD_REQUEST, "租户标识不能为空");
        }
        Long reqTenantId = request.getTenantId();
        if (isLoginLocked(nameForLog, reqTenantId)) {
            sysLoginLogApplicationService.recordFailure(reqTenantId, nameForLog, ip, ua, "登录失败次数过多，账户已临时锁定");
            throw new BusinessException(ResultCode.FORBIDDEN, "登录失败次数过多，请15分钟后重试");
        }

        try {
            loginCaptchaValidator.validate(reqTenantId, request.getCaptchaKey(), request.getCaptcha());
        } catch (BusinessException ex) {
            sysLoginLogApplicationService.recordFailure(reqTenantId, nameForLog, ip, ua, ex.getMessage());
            throw ex;
        }

        LambdaQueryWrapper<SysUser> uq = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim())
                .eq(SysUser::getTenantId, reqTenantId)
                .eq(SysUser::getDelFlag, 0);
        SysUser user = userMapper.selectOne(uq.last("limit 1"));
        if (user == null) {
            sysLoginLogApplicationService.recordFailure(null, nameForLog, ip, ua, "用户名或密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "账号已停用");
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            long failedTimes = increaseLoginFailCount(nameForLog, reqTenantId);
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "用户名或密码错误");
            if (failedTimes >= LOGIN_FAIL_MAX_TIMES) {
                throw new BusinessException(ResultCode.FORBIDDEN, "登录失败次数过多，请15分钟后重试");
            }
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        clearLoginFailCount(nameForLog, reqTenantId);

        TenantContext.setTenantId(user.getTenantId());
        List<AuthDtos.ShopItem> shops = queryShops(user.getId());
        if (shops.isEmpty()) {
            sysLoginLogApplicationService.recordFailure(user.getTenantId(), user.getUsername(), ip, ua, "无可登录店铺");
            throw new BusinessException(ResultCode.FORBIDDEN, "当前用户无可登录店铺");
        }

        Long recommendedShopId = chooseCurrentShop(user.getMainShopId(), shops);
        authRedisService.saveUserShops(user.getId(), shops);
        String preAuthToken = authRedisService.savePreAuthSession(new AuthRedisService.PreAuthSession(
                user.getId(), user.getTenantId(), user.getUsername(), recommendedShopId, shops
        ));

        AuthDtos.PreAuthLoginResponse resp = new AuthDtos.PreAuthLoginResponse();
        resp.setPreAuthToken(preAuthToken);
        resp.setTenantId(user.getTenantId());
        resp.setRecommendedShopId(recommendedShopId);
        resp.setShops(shops);
        resp.setExpiresInSeconds(10 * 60L);
        resp.setRequiresShopSelection(true);
        return resp;
    }

    public AuthDtos.LoginResponse confirmShop(AuthDtos.ConfirmShopRequest request, HttpServletRequest http) {
        String ip = HttpRequestUtils.clientIp(http);
        String ua = http.getHeader("User-Agent");
        if (request == null || !StringUtils.hasText(request.getPreAuthToken())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "preAuthToken 不能为空");
        }
        if (request.getShopId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "shopId 不能为空");
        }

        AuthRedisService.PreAuthSession preAuth = authRedisService.consumePreAuthSession(request.getPreAuthToken())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "登录票据已失效，请重新登录"));

        Long userId = preAuth.userId();
        Long tenantId = preAuth.tenantId();
        String username = preAuth.username();
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDelFlag() != null && user.getDelFlag() == 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已删除");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }

        Long targetShopId = request.getShopId();
        boolean allowed = preAuth.shops() != null && preAuth.shops().stream()
                .map(AuthDtos.ShopItem::getShopId)
                .filter(Objects::nonNull)
                .anyMatch(targetShopId::equals);
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无该店铺权限，无法进入");
        }

        ScopeData scope = scopeByUserAndShop(tenantId, userId, targetShopId);
        authRedisService.saveUserShops(userId, preAuth.shops());
        authRedisService.saveSession(userId,
                new AuthRedisService.SessionState(targetShopId, scope.dataScope, scope.accessibleShopIds));

        SysShop targetShop = shopMapper.selectById(targetShopId);
        Long orgId = targetShop != null ? targetShop.getOrgId() : null;

        Collection<GrantedAuthority> authorities = buildAuthorities(userId, targetShopId, tenantId);
        NexusPrincipal principal = new NexusPrincipal(
                userId, username, tenantId, null, targetShopId, orgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String token = jwtTokenProvider.createAccessToken(principal);

        recordOnlineLoginOrThrow(token, user, ip, ua);

        sysLoginLogApplicationService.recordSuccess(tenantId, username, ip, ua, "登录成功");
        return buildLoginResponse(token, tenantId, targetShopId, orgId, scope);
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
    public AuthDtos.LoginResponse refreshToken(String oldTokenJti, HttpServletRequest http) {
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

        authRedisService.saveSession(userId,
                new AuthRedisService.SessionState(currentShopId, scope.dataScope, scope.accessibleShopIds));

        SysShop currentShop = shopMapper.selectById(currentShopId);
        Long currentOrgId = currentShop != null ? currentShop.getOrgId() : null;

        Collection<GrantedAuthority> authorities = buildAuthorities(userId, currentShopId, tenantId);

        NexusPrincipal principal = new NexusPrincipal(
                userId, username, tenantId, null, currentShopId, currentOrgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String newToken = jwtTokenProvider.createAccessToken(principal);

        // 旧 token 失效（Rotation）
        if (!StringUtils.hasText(oldTokenJti)) {
            oldTokenJti = currentPrincipal.getJti();
        }
        if (StringUtils.hasText(oldTokenJti)) {
            onlineUserRedisService.removeToken(oldTokenJti);
        }

        // 新 token 写入在线会话
        String ip = HttpRequestUtils.clientIp(http);
        String ua = http.getHeader("User-Agent");
        recordOnlineLoginOrThrow(newToken, user, ip, ua);

        sysLoginLogApplicationService.recordSuccess(tenantId, username, ip, ua, "Token 续期");

        AuthDtos.LoginResponse resp = buildLoginResponse(newToken, tenantId, currentShopId, currentOrgId, scope);
        return resp;
    }

    public AuthDtos.LoginResponse switchShop(Long userId, Long tenantId, String username, Long targetShopId,
                                             String oldTokenJti, HttpServletRequest http) {
        if (targetShopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "shopId 不能为空");
        }
        long exists = userShopRoleMapper.selectCount(new LambdaQueryWrapper<SysUserShopRole>()
                .eq(SysUserShopRole::getUserId, userId)
                .eq(SysUserShopRole::getTenantId, tenantId)
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

        Collection<GrantedAuthority> authorities = buildAuthorities(userId, targetShopId, tenantId);

        NexusPrincipal principal = new NexusPrincipal(
                userId, username, tenantId, null, targetShopId, orgId,
                scope.dataScope, scope.accessibleShopIds, scope.accessibleOrgIds, authorities
        );
        String token = jwtTokenProvider.createAccessToken(principal);

        if (StringUtils.hasText(oldTokenJti)) {
            onlineUserRedisService.removeToken(oldTokenJti);
        }
        SysUser user = userMapper.selectById(userId);
        String ip = HttpRequestUtils.clientIp(http);
        String ua = http.getHeader("User-Agent");
        if (user != null) {
            recordOnlineLoginOrThrow(token, user, ip, ua);
        }

        sysLoginLogApplicationService.recordSuccess(tenantId, username, ip, ua, "切换店铺");

        return buildLoginResponse(token, tenantId, targetShopId, orgId, scope);
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

        DataScope maxDataScope = getMaxDataScope(roles.stream()
                .map(SysRole::getDataScope)
                .map(DataScope::fromCode)
                .filter(Objects::nonNull)
                .toList());
        int maxScope = maxDataScope.getCode();

        List<Long> accessibleShopIds = List.of();
        List<Long> accessibleOrgIds = List.of();

        SysShop current = shopMapper.selectById(shopId);
        Long rootOrgId = current != null ? current.getOrgId() : null;

        if (maxScope >= DataScope.SHOP.getCode() && rootOrgId == null) {
            log.warn("店铺 shopId={} 未关联组织(orgId=null)，数据范围降级为 SELF", shopId);
        }

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
        } else if (maxScope == DataScope.SHOP.getCode() && rootOrgId != null) {
            accessibleShopIds = List.of(shopId);
            accessibleOrgIds = List.of(rootOrgId);
        }

        return new ScopeData(maxScope, accessibleShopIds, accessibleOrgIds);
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

    private AuthDtos.LoginResponse buildLoginResponse(String token, Long tenantId, Long currentShopId,
                                                     Long currentOrgId, ScopeData scope) {
        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setAccessToken(token);
        resp.setTokenType("Bearer");
        resp.setTenantId(tenantId);
        resp.setCurrentShopId(currentShopId);
        resp.setCurrentOrgId(currentOrgId);
        resp.setDataScope(scope.dataScope);
        resp.setAccessibleShopIds(scope.accessibleShopIds);
        resp.setAccessibleOrgIds(scope.accessibleOrgIds);
        return resp;
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
    private Collection<GrantedAuthority> buildAuthorities(Long userId, Long shopId, Long tenantId) {
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

        Set<String> permsSet = new java.util.HashSet<>();
        if (!menuIds.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIds)
                    .eq(SysMenu::getDelFlag, 0)
                    .eq(SysMenu::getStatus, 1));
            for (SysMenu menu : menus) {
                if (menu.getPerms() != null && !menu.getPerms().isBlank()) {
                    String p = menu.getPerms().trim();
                    authorities.add(new SimpleGrantedAuthority(p));
                    permsSet.add(p);
                }
            }
        }

        // Cache permissions to Redis for @ss.hasPermi usage — key 包含 tenant+shop 维度
        if (stringRedisTemplate != null) {
            if (roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleCode()))) {
                permsSet.add("*:*:*");
            }
            String scopedKey = "login:permissions:" + tenantId + ":" + userId + ":" + shopId;
            stringRedisTemplate.delete(scopedKey);
            if (!permsSet.isEmpty()) {
                stringRedisTemplate.opsForSet().add(scopedKey, permsSet.toArray(new String[0]));
                stringRedisTemplate.expire(scopedKey, java.time.Duration.ofDays(1));
            }
            // 同时写旧格式 key，保证滚动升级期间兼容
            String legacyKey = "login:permissions:" + userId;
            stringRedisTemplate.delete(legacyKey);
            if (!permsSet.isEmpty()) {
                stringRedisTemplate.opsForSet().add(legacyKey, permsSet.toArray(new String[0]));
                stringRedisTemplate.expire(legacyKey, java.time.Duration.ofDays(1));
            }
        }

        return authorities.stream().distinct().collect(Collectors.toList());
    }

    private record ScopeData(Integer dataScope, List<Long> accessibleShopIds, List<Long> accessibleOrgIds) {
    }

    private boolean isLoginLocked(String username, Long tenantId) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String val = stringRedisTemplate.opsForValue().get(loginFailKey(username, tenantId));
        if (!StringUtils.hasText(val)) {
            return false;
        }
        try {
            return Long.parseLong(val) >= LOGIN_FAIL_MAX_TIMES;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private long increaseLoginFailCount(String username, Long tenantId) {
        if (!StringUtils.hasText(username)) {
            return 0L;
        }
        String key = loginFailKey(username, tenantId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, LOGIN_FAIL_LOCK_TTL);
        }
        return count == null ? 0L : count;
    }

    private void clearLoginFailCount(String username, Long tenantId) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        stringRedisTemplate.delete(loginFailKey(username, tenantId));
    }

    private static String loginFailKey(String username, Long tenantId) {
        return "login:fail:" + tenantId + ":" + username.trim();
    }

    private void recordOnlineLoginOrThrow(String token, SysUser user, String ip, String ua) {
        try {
            onlineUserRedisService.recordLogin(token, user, ip, ua);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "在线会话写入失败，请稍后重试", e);
        }
    }
}
