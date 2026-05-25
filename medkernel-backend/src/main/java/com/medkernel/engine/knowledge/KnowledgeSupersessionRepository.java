package com.medkernel.engine.knowledge;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeSupersessionRepository extends ListCrudRepository<KnowledgeSupersession, Long> {

    List<KnowledgeSupersession> findByTenantIdAndIdentityIdOrderByTransitionedAtAsc(String tenantId, Long identityId);

    @Query("""
        SELECT * FROM knowledge_supersession
        WHERE tenant_id = :tenantId AND identity_id = :identityId
          AND transitioned_at > :sinceExclusive
        ORDER BY transitioned_at ASC, id ASC
        OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<KnowledgeSupersession> lineageAfter(String tenantId, Long identityId,
                                             java.time.Instant sinceExclusive, int limit);
}
