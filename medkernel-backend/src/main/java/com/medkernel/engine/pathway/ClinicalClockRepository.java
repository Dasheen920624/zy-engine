package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalClockRepository extends ListCrudRepository<ClinicalClock, Long> {

    Optional<ClinicalClock> findByClockIdAndTenantId(String clockId, String tenantId);

    List<ClinicalClock> findByPatientPathwayIdAndTenantIdOrderByStartedAtAsc(
        String patientPathwayId, String tenantId);
}
