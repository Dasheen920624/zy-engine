package com.medkernel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全模块配置属性。
 */
@Component
@ConfigurationProperties(prefix = "medkernel.security")
public class SecurityProperties {

    /** JWT 签名密钥，生产环境必须配置为强随机值。 */
    private String jwtSecret = "medkernel-default-jwt-secret-please-change-in-production-env-2026";

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
