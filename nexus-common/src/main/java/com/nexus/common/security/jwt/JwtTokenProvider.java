package com.nexus.common.security.jwt;

import com.nexus.common.security.NexusPrincipal;
import com.nexus.common.security.config.NexusSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_TENANT_ID = "tid";
    public static final String CLAIM_SHOP_ID = "sid";
    public static final String CLAIM_ORG_ID = "oid";
    public static final String CLAIM_DATA_SCOPE = "ds";
    public static final String CLAIM_ACCESSIBLE_SHOP_IDS = "asids";
    public static final String CLAIM_ACCESSIBLE_ORG_IDS = "aoids";
    private static final String CLAIM_AUTHORITIES = "authorities";
    /** 三端标识：web / miniapp / app */
    public static final String CLAIM_CLIENT_TYPE = "clientType";

    private final NexusSecurityProperties securityProperties;

    public JwtTokenProvider(NexusSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 登录签发：7 天有效，Claims 含 userId、tenantId、username(clientType 在 claim 中)。
     */
    @Deprecated
    public String generateLoginToken(Long userId, Long tenantId, String username, String clientType) {
        throw new UnsupportedOperationException("已废弃，请使用 createAccessToken(NexusPrincipal)");
    }

    /**
     * 解析 JWT，失败或过期时抛出 {@link JwtAuthenticationException}。
     */
    public Claims parseTokenClaims(String token) {
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        try {
            return Jwts.parser()
                    .verifyWith(signingKey(jwt.getSecret()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("令牌已过期或无效", ex);
        }
    }

    public String createAccessToken(NexusPrincipal principal) {
        NexusSecurityProperties.Jwt jwt = securityProperties.getJwt();
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwt.getExpirationSeconds() * 1000L);

        List<String> authorityNames = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.getUsername())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, principal.getUserId())
                .claim(CLAIM_TENANT_ID, principal.getTenantId())
                .claim(CLAIM_SHOP_ID, principal.getShopId())
                .claim(CLAIM_ORG_ID, principal.getOrgId())
                .claim(CLAIM_DATA_SCOPE, principal.getDataScope())
                .claim(CLAIM_ACCESSIBLE_SHOP_IDS, principal.getAccessibleShopIds())
                .claim(CLAIM_ACCESSIBLE_ORG_IDS, principal.getAccessibleOrgIds())
                .claim(CLAIM_AUTHORITIES, authorityNames)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey(jwt.getSecret()), Jwts.SIG.HS256)
                .compact();
    }

    public NexusPrincipal parseToken(String token) {
        Claims claims = parseTokenClaims(token);
        return parseToken(claims);
    }

    public NexusPrincipal parseToken(Claims claims) {
        if (claims == null) {
            throw new JwtAuthenticationException("令牌内容为空");
        }

        String username = claims.getSubject();
        String jti = claims.getId();
        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        Long tenantId = claims.get(CLAIM_TENANT_ID, Long.class);
        Long shopId = claims.get(CLAIM_SHOP_ID, Long.class);
        Long orgId = claims.get(CLAIM_ORG_ID, Long.class);
        Integer dataScope = claims.get(CLAIM_DATA_SCOPE, Integer.class);

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_AUTHORITIES, List.class);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String r : roles) {
                authorities.add(new SimpleGrantedAuthority(r));
            }
        }

        @SuppressWarnings("unchecked")
        List<Number> rawShopIds = claims.get(CLAIM_ACCESSIBLE_SHOP_IDS, List.class);
        List<Long> accessibleShopIds = toLongList(rawShopIds);

        @SuppressWarnings("unchecked")
        List<Number> rawOrgIds = claims.get(CLAIM_ACCESSIBLE_ORG_IDS, List.class);
        List<Long> accessibleOrgIds = toLongList(rawOrgIds);

        return new NexusPrincipal(userId, username, tenantId, jti, shopId, orgId, dataScope,
                accessibleShopIds, accessibleOrgIds, authorities);
    }

    private static List<Long> toLongList(List<Number> raw) {
        List<Long> out = new ArrayList<>();
        if (raw != null) {
            for (Number n : raw) {
                if (n != null) {
                    out.add(n.longValue());
                }
            }
        }
        return out;
    }

    private static SecretKey signingKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
