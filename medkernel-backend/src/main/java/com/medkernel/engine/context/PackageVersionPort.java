package com.medkernel.engine.context;

import java.util.Optional;

/**
 * 包版本解析端口。
 *
 * <p>抽象层让 {@link ContextSnapshotService} 不依赖具体的包版本注册中心；
 * 当前默认实现 {@link LenientPackageVersionAdapter}（@ConditionalOnMissingBean）
 * 沿用"非空即合法"行为；API-10 包发布 API 落地后引入
 * {@code KnowledgePackageVersionAdapter} 通过 {@code @Primary} 自动覆盖。
 */
public interface PackageVersionPort {

    /** 包版本是否存在（exists 表示业务可用，含发布、灰度等状态由实现自行决定）。 */
    boolean exists(String tenantId, String packageType, String version);

    /** 当前 tenant + packageType 的活跃版本；未注册返回 empty。 */
    Optional<String> getActive(String tenantId, String packageType);
}
