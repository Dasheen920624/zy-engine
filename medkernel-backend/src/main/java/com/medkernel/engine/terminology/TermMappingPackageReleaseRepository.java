package com.medkernel.engine.terminology;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermMappingPackageReleaseRepository extends ListCrudRepository<TermMappingPackageRelease, Long> {
}
