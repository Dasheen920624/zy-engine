package com.medkernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务（等保 2.0 三级 - 访问控制）。
 *
 * 提供：
 * - JWT Token 黑名单（登出/强制下线时加入）
 * - 会话超时控制（默认 8 小时）
 * - 并发会话限制（默认每用户 5 个活跃会话）
 * - 定时清理过期会话
 */
@Service
public class SessionManagementService {

    private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

    /** 黑名单：tokenId -> 过期时间戳 */
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<String, Long>();

    /** 活跃会话：tokenId -> SessionInfo */
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<String, SessionInfo>();

    /** 用户会话索引：userId -> tokenId 集合 */
    private final Map<Long, Map<String, SessionInfo>> userSessions = new ConcurrentHashMap<Long, Map<String, SessionInfo>>();

    private static final long SESSION_TIMEOUT_MS = 8 * 60 * 60 * 1000L; // 8 小时
    private static final int MAX_CONCURRENT_SESSIONS = 5;

    /**
     * 将 Token 加入黑名单（登出/强制下线）。
     */
    public void blacklistToken(String tokenId, long expiresAtMillis) {
        tokenBlacklist.put(tokenId, expiresAtMillis);
        activeSessions.remove(tokenId);
        log.info("[session] token blacklisted: tokenId={}", tokenId);
    }

    /**
     * 检查 Token 是否在黑名单中。
     */
    public boolean isBlacklisted(String tokenId) {
        Long expiresAt = tokenBlacklist.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            tokenBlacklist.remove(tokenId);
            return false;
        }
        return true;
    }

    /**
     * 注册新会话。
     *
     * @return true 如果注册成功，false 如果超过并发会话限制
     */
    public boolean registerSession(String tokenId, Long userId, String username, String clientIp) {
        SessionInfo session = new SessionInfo();
        session.tokenId = tokenId;
        session.userId = userId;
        session.username = username;
        session.clientIp = clientIp;
        session.createdAt = System.currentTimeMillis();
        session.lastAccessedAt = System.currentTimeMillis();

        Map<String, SessionInfo> sessions = userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<String, SessionInfo>());

        // 清理过期会话
        cleanupUserSessions(sessions);

        // 检查并发会话数
        if (sessions.size() >= MAX_CONCURRENT_SESSIONS) {
            // 踢出最旧的会话
            String oldestToken = findOldestSession(sessions);
            if (oldestToken != null) {
                sessions.remove(oldestToken);
                activeSessions.remove(oldestToken);
                log.info("[session] evicted oldest session for userId={}, tokenId={}", userId, oldestToken);
            }
        }

        sessions.put(tokenId, session);
        activeSessions.put(tokenId, session);
        return true;
    }

    /**
     * 更新会话最后访问时间。
     */
    public void touchSession(String tokenId) {
        SessionInfo session = activeSessions.get(tokenId);
        if (session != null) {
            session.lastAccessedAt = System.currentTimeMillis();
        }
    }

    /**
     * 强制用户下线（踢出所有会话）。
     */
    public int forceLogout(Long userId) {
        Map<String, SessionInfo> sessions = userSessions.remove(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        int count = sessions.size();
        for (String tokenId : sessions.keySet()) {
            tokenBlacklist.put(tokenId, System.currentTimeMillis() + SESSION_TIMEOUT_MS);
            activeSessions.remove(tokenId);
        }
        log.info("[session] force logout: userId={}, sessions={}", userId, count);
        return count;
    }

    /**
     * 获取用户活跃会话数。
     */
    public int getActiveSessionCount(Long userId) {
        Map<String, SessionInfo> sessions = userSessions.get(userId);
        if (sessions == null) return 0;
        cleanupUserSessions(sessions);
        return sessions.size();
    }

    /**
     * 定时清理过期黑名单和会话（每 5 分钟）。
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L)
    public void cleanup() {
        long now = System.currentTimeMillis();

        // 清理过期黑名单
        Iterator<Map.Entry<String, Long>> blIt = tokenBlacklist.entrySet().iterator();
        while (blIt.hasNext()) {
            if (blIt.next().getValue() < now) {
                blIt.remove();
            }
        }

        // 清理过期会话
        Iterator<Map.Entry<String, SessionInfo>> sessIt = activeSessions.entrySet().iterator();
        while (sessIt.hasNext()) {
            SessionInfo session = sessIt.next().getValue();
            if (now - session.lastAccessedAt > SESSION_TIMEOUT_MS) {
                sessIt.remove();
                Map<String, SessionInfo> userSess = userSessions.get(session.userId);
                if (userSess != null) {
                    userSess.remove(session.tokenId);
                }
            }
        }
    }

    private void cleanupUserSessions(Map<String, SessionInfo> sessions) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, SessionInfo>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            SessionInfo session = it.next().getValue();
            if (now - session.lastAccessedAt > SESSION_TIMEOUT_MS) {
                it.remove();
                activeSessions.remove(session.tokenId);
            }
        }
    }

    private String findOldestSession(Map<String, SessionInfo> sessions) {
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<String, SessionInfo> entry : sessions.entrySet()) {
            if (entry.getValue().createdAt < oldestTime) {
                oldestTime = entry.getValue().createdAt;
                oldest = entry.getKey();
            }
        }
        return oldest;
    }

    /**
     * 会话信息。
     */
    static class SessionInfo {
        String tokenId;
        Long userId;
        String username;
        String clientIp;
        long createdAt;
        long lastAccessedAt;
    }
}
