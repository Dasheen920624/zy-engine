package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyProfileRepository extends ListCrudRepository<SpecialtyProfile, Long> {

    Optional<SpecialtyProfile> findByProfileIdAndTenantId(String profileId, String tenantId);

    List<SpecialtyProfile> findByPackageIdAndTenantIdOrderByProfileCodeAsc(String packageId, String tenantId);
}
