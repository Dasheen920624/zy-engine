package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PathwayEdgeRepository extends ListCrudRepository<PathwayEdge, Long> {

    Optional<PathwayEdge> findByEdgeIdAndTenantId(String edgeId, String tenantId);

    List<PathwayEdge> findByTemplateIdAndTenantIdOrderByPriorityAsc(String templateId, String tenantId);

    List<PathwayEdge> findByTemplateIdAndTenantIdAndFromNodeCodeOrderByPriorityAsc(
        String templateId, String tenantId, String fromNodeCode);
}
