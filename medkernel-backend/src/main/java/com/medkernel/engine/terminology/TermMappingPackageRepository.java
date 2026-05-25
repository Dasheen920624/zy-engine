package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermMappingPackageRepository extends ListCrudRepository<TermMappingPackage, Long> {

    Optional<TermMappingPackage> findByTenantIdAndId(String tenantId, Long id);

    @Query("""
        SELECT COUNT(*) FROM term_mapping_package
        WHERE tenant_id = :tenantId
          AND (:packageCode IS NULL OR package_code = :packageCode)
          AND (:status IS NULL OR status = :status)
          AND (:scopeLevel IS NULL OR scope_level = :scopeLevel)
          AND (:scopeCode IS NULL OR scope_code = :scopeCode)
        """)
    long countByFilter(String tenantId, String packageCode, String status, String scopeLevel, String scopeCode);

    @Query("""
        SELECT * FROM term_mapping_package
        WHERE tenant_id = :tenantId
          AND (:packageCode IS NULL OR package_code = :packageCode)
          AND (:status IS NULL OR status = :status)
          AND (:scopeLevel IS NULL OR scope_level = :scopeLevel)
          AND (:scopeCode IS NULL OR scope_code = :scopeCode)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<TermMappingPackage> pageByFilter(String tenantId, String packageCode, String status, String scopeLevel,
                                          String scopeCode, int offset, int limit);

    @Query("""
        SELECT * FROM term_mapping_package
        WHERE tenant_id = :tenantId
          AND package_code = :packageCode
          AND scope_level = :scopeLevel
          AND scope_code = :scopeCode
          AND status IN ('GRAY','PUBLISHED')
        ORDER BY published_at DESC, id DESC
        """)
    List<TermMappingPackage> findActiveByTenantIdAndPackageCodeAndScope(String tenantId, String packageCode,
                                                                        String scopeLevel, String scopeCode);
}
