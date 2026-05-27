package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientPathwayRepository extends ListCrudRepository<PatientPathway, Long> {

    Optional<PatientPathway> findByPatientPathwayIdAndTenantId(String patientPathwayId, String tenantId);

    List<PatientPathway> findByTemplateIdAndTenantIdOrderByEnteredAtDesc(String templateId, String tenantId);
}
