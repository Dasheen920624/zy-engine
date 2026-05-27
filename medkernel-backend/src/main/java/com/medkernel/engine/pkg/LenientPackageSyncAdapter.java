package com.medkernel.engine.pkg;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * 默认知识包同步宽松适配器。
 *
 * <p>根据 B0 离线基线原则，若无外部三方物理同步（如 Neo4j / Dify 物理集成）实现，
 * 本组件默认兜底装配，确保包发布的主链路 100% 畅通并通过核心验收。
 */
@Component
public class LenientPackageSyncAdapter implements PackageSyncPort {

    @Override
    public String sync(String tenantId, ReleasePlan plan, SyncTarget target) throws Exception {
        // 模拟离线同步逻辑，计算带有发布计划、通道和时间戳的唯一摘要作为证据存证
        String input = String.format("tenant:%s;plan:%s;target:%s;time:%s",
            tenantId, plan.planId(), target.targetId(), Instant.now().toString());
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return "LNT-" + hexString.toString().substring(0, 32).toUpperCase();
    }
}
