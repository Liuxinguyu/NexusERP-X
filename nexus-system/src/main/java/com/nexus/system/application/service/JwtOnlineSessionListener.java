package com.nexus.system.application.service;

import com.nexus.common.security.event.JwtTokenAuthenticatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JwtOnlineSessionListener {

    private final OnlineUserRedisService onlineUserRedisService;

    public JwtOnlineSessionListener(OnlineUserRedisService onlineUserRedisService) {
        this.onlineUserRedisService = onlineUserRedisService;
    }

    @EventListener
    public void onAuthenticated(JwtTokenAuthenticatedEvent event) {
        if (event.getPrincipal() != null) {
            onlineUserRedisService.refreshTokenTtl(event.getPrincipal().getJti());
        }
    }
}
