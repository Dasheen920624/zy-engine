package com.medkernel.engine.terminology;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 术语映射包发布事件持久化仓库；事件流水仅追加写入。
 */
@Repository
public interface TermMappingPackageReleaseRepository extends ListCrudRepository<TermMappingPackageRelease, Long> {
}
