package com.nexus.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static NexusPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object p = authentication.getPrincipal();
        if (p instanceof NexusPrincipal nexusPrincipal) {
            return nexusPrincipal;
        }
        return null;
    }

    public static Long currentUserId() {
        NexusPrincipal p = currentPrincipal();
        return p == null ? null : p.getUserId();
    }

    public static Long currentShopId() {
        NexusPrincipal p = currentPrincipal();
        return p == null ? null : p.getShopId();
    }

    public static Long currentOrgId() {
        NexusPrincipal p = currentPrincipal();
        return p == null ? null : p.getOrgId();
    }
}


