package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PathwayTemplateRepository extends ListCrudRepository<PathwayTemplate, Long> {

    Optional<PathwayTemplate> findByTemplateIdAndTenantId(String templateId, String tenantId);

    Optional<PathwayTemplate> findByTenantIdAndTemplateCodeAndTemplateVersion(
        String tenantId, String templateCode, Integer templateVersion);

    @Query("""
        SELECT * FROM pathway_template
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:diseaseCode IS NULL OR disease_code = :diseaseCode)
          AND (:packageId IS NULL OR package_id = :packageId)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<PathwayTemplate> pageByFilter(String tenantId, String status, String diseaseCode,
                                       String packageId, int offset, int limit);

    @Query("""
        SELECT COUNT(*) FROM pathway_template
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:diseaseCode IS NULL OR disease_code = :diseaseCode)
          AND (:packageId IS NULL OR package_id = :packageId)
        """)
    long countByFilter(String tenantId, String status, String diseaseCode, String packageId);
}
