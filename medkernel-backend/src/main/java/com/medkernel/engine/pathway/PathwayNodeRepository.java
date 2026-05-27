package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PathwayNodeRepository extends ListCrudRepository<PathwayNode, Long> {

    Optional<PathwayNode> findByNodeIdAndTenantId(String nodeId, String tenantId);

    Optional<PathwayNode> findByTemplateIdAndTenantIdAndNodeCode(
        String templateId, String tenantId, String nodeCode);

    List<PathwayNode> findByTemplateIdAndTenantIdOrderBySortOrderAsc(String templateId, String tenantId);
}
