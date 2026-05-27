package com.medkernel.engine.rule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleExecutionLogRepository extends ListCrudRepository<RuleExecutionLog, Long> {

    Optional<RuleExecutionLog> findByExecutionIdAndTenantId(String executionId, String tenantId);

    @Query("""
        SELECT * FROM rule_execution_log
        WHERE tenant_id = :tenantId AND rule_id = :ruleId
        ORDER BY executed_at DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<RuleExecutionLog> pageByRule(String tenantId, String ruleId, int offset, int limit);
}
