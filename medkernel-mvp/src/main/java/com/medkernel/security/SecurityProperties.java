package com.medkernel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全模块配置属性。
 */
@Component
@ConfigurationProperties(prefix = "medkernel.security")
public class SecurityProperties {

    /**
     * JWT 签名密钥。
     *
     * <p>必须通过环境变量 {@code MEDKERNEL_JWT_SECRET} 注入，长度 >= 32 字符。
     * 不再在 Java/yml 中保留默认值，避免源码可读的弱密钥被用于签发 JWT（AUDIT §5.1）。
     * 启动校验在 {@link JwtTokenProvider#init()}：null/空 → 抛 IllegalStateException 阻止启动。
     * 测试场景请使用 {@code application-test.yml} 提供测试专用密钥。
     */
    private String jwtSecret;

    /** 登录失败锁定阈值，超过此次数将锁定账户。 */
    private int lockThreshold = 5;

    /** 锁定持续时间（分钟）。 */
    private int lockDurationMinutes = 30;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getLockThreshold() {
        return lockThreshold;
    }

    public void setLockThreshold(int lockThreshold) {
        this.lockThreshold = lockThreshold;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(int lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }
}
