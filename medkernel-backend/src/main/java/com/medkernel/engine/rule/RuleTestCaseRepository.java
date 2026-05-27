package com.medkernel.engine.rule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleTestCaseRepository extends ListCrudRepository<RuleTestCase, Long> {

    Optional<RuleTestCase> findByCaseIdAndTenantId(String caseId, String tenantId);

    @Query("""
        SELECT * FROM rule_test_case
        WHERE version_id = :versionId AND tenant_id = :tenantId
        ORDER BY created_at ASC, id ASC
        """)
    List<RuleTestCase> findByVersionIdAndTenantIdOrderByCreatedAtAsc(String versionId, String tenantId);

    @Query("""
        SELECT DISTINCT case_type FROM rule_test_case
        WHERE version_id = :versionId AND tenant_id = :tenantId
        """)
    List<String> findCoveredTypes(String versionId, String tenantId);
}
