package com.medkernel.engine.rule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 规则版本持久化仓库（GA-ENG-API-05）。
 *
 * <p>用于按 {@code version_id} 装载当前激活版本、按 {@code rule_id} 拉取历史版本列表与最新指定状态版本。
 */
@Repository
public interface RuleVersionRepository extends ListCrudRepository<RuleVersion, Long> {

    /**
     * 按版本业务 ID 与租户 ID 查询规则版本，用于当前激活版本装载。
     */
    Optional<RuleVersion> findByVersionIdAndTenantId(String versionId, String tenantId);

    /**
     * 按规则 ID、租户 ID 与版本号查询指定规则版本，用于后续版本化回放或灰度扩展。
     */
    Optional<RuleVersion> findByRuleIdAndTenantIdAndVersionNo(String ruleId, String tenantId, Integer versionNo);

    /**
     * 按规则按 {@code version_no} 倒序列出全部历史版本，用于版本审计。
     */
    @Query("""
        SELECT * FROM rule_version
        WHERE tenant_id = :tenantId AND rule_id = :ruleId
        ORDER BY version_no DESC
        """)
    List<RuleVersion> findByRuleIdAndTenantIdOrderByVersionNoDesc(String ruleId, String tenantId);

    /**
     * 查询指定规则在指定状态下的最新版本（按 {@code version_no} 倒序取首条）。
     */
    @Query("""
        SELECT * FROM rule_version
        WHERE tenant_id = :tenantId AND rule_id = :ruleId AND status = :status
        ORDER BY version_no DESC
        OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
        """)
    Optional<RuleVersion> findLatestByRuleIdTenantAndStatus(String tenantId, String ruleId, String status);
}
