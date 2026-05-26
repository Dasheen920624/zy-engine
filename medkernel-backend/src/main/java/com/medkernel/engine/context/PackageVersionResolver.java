package com.medkernel.engine.context;

import org.springframework.stereotype.Component;

/**
 * 包版本解析。
 *
 * <p>本任务以"非空即合法"作为最小实现；待 API-10 包发布 API 落地后接其真实查询。
 * 通过该组件单独隔离，未来切换实现不动 {@code ContextSnapshotService}。
 */
@Component
public class PackageVersionResolver {

    public boolean exists(String tenantId, String packageType, String version) {
        return version != null && !version.isBlank();
    }
}
