package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyPackageRepository extends ListCrudRepository<SpecialtyPackage, Long> {

    Optional<SpecialtyPackage> findByPackageIdAndTenantId(String packageId, String tenantId);

    Optional<SpecialtyPackage> findByTenantIdAndPackageCodeAndPackageVersion(
        String tenantId, String packageCode, String packageVersion);

    List<SpecialtyPackage> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    @Query("""
        SELECT * FROM specialty_package
        WHERE tenant_id = :tenantId
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<SpecialtyPackage> pageByTenantId(String tenantId, int offset, int limit);

    @Query("""
        SELECT COUNT(*) FROM specialty_package
        WHERE tenant_id = :tenantId
        """)
    long countByTenantId(String tenantId);
}
