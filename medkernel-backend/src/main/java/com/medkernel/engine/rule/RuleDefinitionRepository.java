package com.medkernel.engine.rule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleDefinitionRepository extends ListCrudRepository<RuleDefinition, Long> {

    Optional<RuleDefinition> findByRuleIdAndTenantId(String ruleId, String tenantId);

    Optional<RuleDefinition> findByTenantIdAndRuleCode(String tenantId, String ruleCode);

    @Query("""
        SELECT * FROM rule_definition
        WHERE tenant_id = :tenantId AND status = 'PUBLISHED'
        ORDER BY updated_at DESC, id DESC
        """)
    List<RuleDefinition> findPublishedByTenantId(String tenantId);

    @Query("""
        SELECT * FROM rule_definition
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:ruleType IS NULL OR rule_type = :ruleType)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<RuleDefinition> pageByFilter(String tenantId, String status, String ruleType,
                                      String riskLevel, int offset, int limit);

    @Query("""
        SELECT COUNT(*) FROM rule_definition
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:ruleType IS NULL OR rule_type = :ruleType)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
        """)
    long countByFilter(String tenantId, String status, String ruleType, String riskLevel);
}
