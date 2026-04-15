package com.nexus.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NexusPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final Long tenantId;
    private final Long shopId;
    private final Long orgId;
    private final Integer dataScope;
    private final List<Long> accessibleShopIds;
    private final List<Long> accessibleOrgIds;
    private final Collection<? extends GrantedAuthority> authorities;

    public NexusPrincipal(Long userId, String username, Long tenantId, Long shopId, Long orgId, Integer dataScope,
                          List<Long> accessibleShopIds, List<Long> accessibleOrgIds,
                          Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.shopId = shopId;
        this.orgId = orgId;
        this.dataScope = dataScope;
        this.accessibleShopIds = accessibleShopIds == null ? List.of() : List.copyOf(accessibleShopIds);
        this.accessibleOrgIds = accessibleOrgIds == null ? List.of() : List.copyOf(accessibleOrgIds);
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getShopId() {
        return shopId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Integer getDataScope() {
        return dataScope;
    }

    public List<Long> getAccessibleShopIds() {
        return accessibleShopIds;
    }

    public List<Long> getAccessibleOrgIds() {
        return accessibleOrgIds;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
