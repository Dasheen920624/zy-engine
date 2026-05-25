package com.medkernel.engine.terminology;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermMappingPackageItemRepository extends ListCrudRepository<TermMappingPackageItem, Long> {
}
