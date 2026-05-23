package com.medkernel.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nonce 防重放服务。
 * <p>
 * SEC-003: 缓存已使用的 nonce，在 TTL 窗口内拒绝重复请求。
 */
public class NonceService {

    private final ConcurrentHashMap<String, Instant> nonceCache = new ConcurrentHashMap<String, Instant>();
    private final Duration ttl;
    private volatile Instant lastCleanup = Instant.now();
    private final Duration cleanupInterval = Duration.ofMinutes(5);

    public NonceService() {
        this(Duration.ofMinutes(5));
    }

    public NonceService(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * 检查 nonce 是否有效（未被使用且在 TTL 内）。
     *
     * @param nonce     请求中的 nonce 值
     * @param timestamp 请求时间戳（毫秒），必须与 nonce 一起传入以验证时效
     * @return true 如果 nonce 有效且未被使用
     */
    public boolean validateAndRecord(String nonce, long timestamp) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }

        // 时间戳验证：请求不能超过 TTL 窗口
        Instant requestTime = Instant.ofEpochMilli(timestamp);
        Instant now = Instant.now();
        if (requestTime.isAfter(now.plus(Duration.ofSeconds(30)))) {
            return false; // 未来时间超过 30 秒容差
        }
        if (now.minus(ttl).isAfter(requestTime)) {
            return false; // 请求已过期
        }

        // Nonce 唯一性检查
        Instant existing = nonceCache.putIfAbsent(nonce, Instant.now());
        if (existing != null) {
            return false; // nonce 已使用
        }

        // 定期清理过期 nonce
        maybeCleanup();
        return true;
    }

    private void maybeCleanup() {
        Instant now = Instant.now();
        if (now.minus(cleanupInterval).isAfter(lastCleanup)) {
            lastCleanup = now;
            Instant cutoff = now.minus(ttl);
            nonceCache.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        }
    }

    /**
     * 获取缓存统计信息（用于监控）。
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<String, Object>();
        stats.put("cached_nonces", nonceCache.size());
        stats.put("ttl_seconds", ttl.getSeconds());
        stats.put("last_cleanup", lastCleanup.toString());
        return stats;
    }
}
