package com.medkernel.engine.terminology;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 术语映射包条目持久化仓库；写多读少，主要用于构包写入与回查。
 */
@Repository
public interface TermMappingPackageItemRepository extends ListCrudRepository<TermMappingPackageItem, Long> {
}
