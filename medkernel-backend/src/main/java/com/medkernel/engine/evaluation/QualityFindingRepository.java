package com.medkernel.engine.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityFindingRepository extends ListCrudRepository<QualityFinding, Long> {

    Optional<QualityFinding> findByFindingIdAndTenantId(String findingId, String tenantId);

    List<QualityFinding> findByResultIdAndTenantIdOrderByCreatedAtAsc(String resultId, String tenantId);

    List<QualityFinding> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
