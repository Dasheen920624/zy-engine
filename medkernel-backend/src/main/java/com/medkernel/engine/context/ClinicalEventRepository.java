package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalEventRepository extends ListCrudRepository<ClinicalEvent, Long> {

    Optional<ClinicalEvent> findByEventIdAndTenantId(String eventId, String tenantId);
}
