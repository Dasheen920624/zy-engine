package com.medkernel.engine.evaluation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RectificationTaskRepository extends ListCrudRepository<RectificationTask, Long> {

    Optional<RectificationTask> findByFindingIdAndTenantId(String findingId, String tenantId);
}
