package com.nexus.common.security.event;

import com.nexus.common.security.NexusPrincipal;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * JWT 校验通过后发布，用于在线会话续期等（避免 common 模块依赖业务实现）。
 */
@Getter
public class JwtTokenAuthenticatedEvent extends ApplicationEvent {

    private final String rawToken;
    private final NexusPrincipal principal;
    private final String clientIp;
    private final String userAgent;

    public JwtTokenAuthenticatedEvent(Object source, String rawToken, NexusPrincipal principal,
                                      String clientIp, String userAgent) {
        super(source);
        this.rawToken = rawToken;
        this.principal = principal;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
}
