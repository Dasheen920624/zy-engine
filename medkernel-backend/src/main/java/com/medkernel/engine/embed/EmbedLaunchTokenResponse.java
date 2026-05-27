package com.medkernel.engine.embed;

import java.time.Instant;

/**
 * 嵌入启动令牌生成响应数据契约 (GA-ENG-API-11)。
 */
public record EmbedLaunchTokenResponse(
    String token,
    Instant expiredAt,
    String embedUrl
) {}
