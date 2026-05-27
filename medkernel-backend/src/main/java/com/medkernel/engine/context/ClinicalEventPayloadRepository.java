package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalEventPayloadRepository extends ListCrudRepository<ClinicalEventPayload, Long> {

    Optional<ClinicalEventPayload> findByEventIdAndTenantId(String eventId, String tenantId);
}
