package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * MedKernel v1.0 GA · GA-ENG-API-01b 默认包版本端口适配。
 *
 * <p>沿用旧 PackageVersionResolver "非空即合法" 行为；当 API-10 引入真实
 * KnowledgePackageVersionAdapter 时通过 {@code @Primary} 自动覆盖本默认实现。
 */
@Component
@ConditionalOnMissingBean(PackageVersionPort.class)
public class LenientPackageVersionAdapter implements PackageVersionPort {

    @Override
    public boolean exists(String tenantId, String packageType, String version) {
        return version != null && !version.isBlank();
    }

    @Override
    public Optional<String> getActive(String tenantId, String packageType) {
        // Lenient 实现没有真实包注册中心，返回 empty 让上游决策 fail-fast
        return Optional.empty();
    }
}
