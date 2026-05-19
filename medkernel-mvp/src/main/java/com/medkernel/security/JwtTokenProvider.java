package com.medkernel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 令牌提供者：负责签发和验证 JWT。
 * JWT 中包含 platform_user_id（不变量 #2）和 tenant_id。
 */
@Component
public class JwtTokenProvider {

    private static final long TOKEN_VALIDITY_MS = 8 * 60 * 60 * 1000L; // 8 hours
    private static final String CLAIM_USER_ID = "platform_user_id";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_DISPLAY_NAME = "display_name";

    private final SecurityProperties properties;
    private Key signingKey;

    public JwtTokenProvider(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 JWT 令牌。
     *
     * @param userId      用户 ID（platform_user_id）
     * @param tenantId    租户 ID
     * @param username    用户名
     * @param displayName 显示名
     * @return 签发的 JWT 字符串
     */
    public String createToken(Long userId, Long tenantId, String username, String displayName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TOKEN_VALIDITY_MS);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_DISPLAY_NAME, displayName)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 JWT 令牌。
     *
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws JwtException 如果令牌无效或过期
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Claims 中提取 platform_user_id。
     */
    public Long getUserId(Claims claims) {
        Object value = claims.get(CLAIM_USER_ID);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 从 Claims 中提取 tenant_id。
     */
    public Long getTenantId(Claims claims) {
        Object value = claims.get(CLAIM_TENANT_ID);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 从 Claims 中提取 username。
     */
    public String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 验证令牌是否有效（未过期且签名正确）。
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
