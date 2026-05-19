package com.medkernel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final long TOKEN_VALIDITY_MS = 8 * 60 * 60 * 1000L; // 8 hours
    private static final String CLAIM_USER_ID = "platform_user_id";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_DISPLAY_NAME = "display_name";

    /**
     * 已知默认密钥（与 SecurityProperties / application.yml 同步）。
     * 启动时若仍为该值需要打出 WARN，提醒运维设置 MEDKERNEL_JWT_SECRET。
     * 见 docs/engineering/AUDIT-20260519-V2重构后全量代码审计.md §5.1。
     */
    private static final String DEFAULT_INSECURE_SECRET =
            "medkernel-default-jwt-secret-please-change-in-production-env-2026";

    private static final int MIN_SECRET_LENGTH = 32;

    private final SecurityProperties properties;
    private Key signingKey;

    public JwtTokenProvider(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                    "MEDKERNEL_JWT_SECRET 未配置；启动被阻止。请在环境变量中设置长度 >= "
                            + MIN_SECRET_LENGTH + " 的强随机密钥。");
        }
        if (DEFAULT_INSECURE_SECRET.equals(secret)) {
            log.warn("[security] MEDKERNEL_JWT_SECRET 仍为代码内置默认值，存在被伪造 JWT 风险；"
                    + "生产/UAT 部署前必须通过环境变量覆盖。详见 AUDIT-20260519 §5.1。");
        } else if (secret.length() < MIN_SECRET_LENGTH) {
            log.warn("[security] MEDKERNEL_JWT_SECRET 长度 {} 小于建议值 {}，签名强度不足。",
                    secret.length(), MIN_SECRET_LENGTH);
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
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
