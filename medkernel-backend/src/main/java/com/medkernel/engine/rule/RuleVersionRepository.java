package com.medkernel.engine.rule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleVersionRepository extends ListCrudRepository<RuleVersion, Long> {

    Optional<RuleVersion> findByVersionIdAndTenantId(String versionId, String tenantId);

    Optional<RuleVersion> findByRuleIdAndTenantIdAndVersionNo(String ruleId, String tenantId, Integer versionNo);

    @Query("""
        SELECT * FROM rule_version
        WHERE tenant_id = :tenantId AND rule_id = :ruleId
        ORDER BY version_no DESC
        """)
    List<RuleVersion> findByRuleIdAndTenantIdOrderByVersionNoDesc(String ruleId, String tenantId);

    @Query("""
        SELECT * FROM rule_version
        WHERE tenant_id = :tenantId AND rule_id = :ruleId AND status = :status
        ORDER BY version_no DESC
        OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
        """)
    Optional<RuleVersion> findLatestByRuleIdTenantAndStatus(String tenantId, String ruleId, String status);
}
