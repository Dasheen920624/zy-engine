package com.medkernel.engine.pathway;

import java.util.Optional;
import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyPackageRepository extends ListCrudRepository<SpecialtyPackage, Long> {

    Optional<SpecialtyPackage> findByPackageIdAndTenantId(String packageId, String tenantId);

    Optional<SpecialtyPackage> findByTenantIdAndPackageCodeAndPackageVersion(
        String tenantId, String packageCode, String packageVersion);

    List<SpecialtyPackage> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
