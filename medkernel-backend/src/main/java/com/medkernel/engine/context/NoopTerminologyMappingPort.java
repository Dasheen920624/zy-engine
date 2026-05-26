package com.medkernel.engine.context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 字典映射端口的默认空实现，所有类型返回 UNKNOWN。
 *
 * <p>当 terminology 模块尚未提供 {@link TerminologyMappingPort} Bean 时启用，
 * 不阻断 snapshot 创建，便于 API-01 单独验收。
 */
@Component
@ConditionalOnMissingBean(TerminologyMappingPort.class)
class NoopTerminologyMappingPort implements TerminologyMappingPort {

    @Override
    public Map<String, String> evaluate(String tenantId,
            Map<CanonicalResourceType, List<String>> snapshotSummary) {
        return snapshotSummary.keySet().stream()
            .collect(Collectors.toMap(t -> t.name() + ".code", t -> "UNKNOWN"));
    }
}
