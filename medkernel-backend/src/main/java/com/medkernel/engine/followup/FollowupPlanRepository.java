package com.medkernel.engine.followup;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * 随访计划存储库。
 */
public interface FollowupPlanRepository extends CrudRepository<FollowupPlan, Long>, PagingAndSortingRepository<FollowupPlan, Long> {
    Optional<FollowupPlan> findByPlanId(String planId);
    Page<FollowupPlan> findByTenantIdAndPatientId(String tenantId, String patientId, Pageable pageable);
}
