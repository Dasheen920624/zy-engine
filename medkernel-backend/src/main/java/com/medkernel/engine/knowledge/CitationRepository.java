package com.medkernel.engine.knowledge;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationRepository extends ListCrudRepository<Citation, Long> {

    List<Citation> findByTenantIdAndAssetVersionIdOrderByWeightDescIdAsc(String tenantId, Long assetVersionId);
}
