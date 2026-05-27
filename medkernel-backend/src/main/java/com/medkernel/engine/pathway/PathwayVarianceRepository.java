package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PathwayVarianceRepository extends ListCrudRepository<PathwayVariance, Long> {

    Optional<PathwayVariance> findByVarianceIdAndTenantId(String varianceId, String tenantId);

    List<PathwayVariance> findByPatientPathwayIdAndTenantIdOrderByCreatedAtAsc(
        String patientPathwayId, String tenantId);
}
