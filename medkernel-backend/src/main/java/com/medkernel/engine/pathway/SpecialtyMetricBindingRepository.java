package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyMetricBindingRepository extends ListCrudRepository<SpecialtyMetricBinding, Long> {

    Optional<SpecialtyMetricBinding> findByBindingIdAndTenantId(String bindingId, String tenantId);

    List<SpecialtyMetricBinding> findByTemplateIdAndTenantIdOrderByNodeCodeAsc(
        String templateId, String tenantId);
}
