package com.medkernel.engine.knowledge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 来源文件 Repository。所有查询强制带 tenantId。
 */
@Repository
public interface SourceDocumentRepository extends ListCrudRepository<SourceDocument, Long> {

    Optional<SourceDocument> findByTenantIdAndSourceCode(String tenantId, String sourceCode);

    Optional<SourceDocument> findByTenantIdAndId(String tenantId, Long id);

    List<SourceDocument> findByTenantIdAndSourceTypeOrderByUpdatedAtDesc(String tenantId, SourceType sourceType);

    @Query("SELECT COUNT(*) FROM source_document WHERE tenant_id = :tenantId")
    long countByTenantId(String tenantId);

    @Query("""
        SELECT * FROM source_document
        WHERE tenant_id = :tenantId
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<SourceDocument> pageByTenantId(String tenantId, int offset, int limit);
}
